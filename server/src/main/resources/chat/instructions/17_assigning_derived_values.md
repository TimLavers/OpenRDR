# Assigning derived values

A derived attribute (also known as a "derived value") is a named value that the knowledge base can compute or assign to
a case. Examples:

- "Diabetes status" assigned the value `"diabetic"`
- "Pulse pressure" calculated as `systolic - diastolic`
- "Risk level" assigned `"low"`

Use the operations below when the user wants to create, remove, or change such a value.

## Step 1: Identify the intent

Common intents that trigger a derived-value action:

- "Assign <value> to <attribute>"
- "Record that <attribute> is <value>"
- "Note that <attribute> = <value>"
- "Calculate <attribute> as <formula>"
- "Add a BMI calculation"
- "Remove the derived value <attribute>"
- "Add derived attribute <attribute> with value <value>"
- "Remove <attribute>" (where <attribute> refers to a derived attribute)
- "Replace <attribute>" (where <attribute> refers to a derived attribute)

Note: The user can use the terms "Add", "Remove", and "Replace" to refer to derived attributes as well as comments. If
the user uses these terms, examine what the user is trying to add, remove, or replace. If the following term refers to
an existing attribute (case insensitive), you can assume the user wishes to remove or replace that derived attribute.
Otherwise, they are referring to a comment action. If in doubt, ask for clarification.

If the user wants to add, remove or replace a comment, follow "Defining the change to the report" instead.

If the user wants a *global* correction to the formula or value of an existing derived attribute (e.g. "fix the BMI
formula"), with no condition attached, follow "Editing the definition of a derived attribute" instead — that is an
in-place edit, not a rule.

## Step 2: Obtain the attribute name

If the user does not provide a clear attribute name, propose a short semantic name and ask for confirmation.

Examples:

- User: "Note that this patient is diabetic." → Ask: "What should the derived attribute be called? For example, '
  Diabetes status'."
- User: "Calculate BMI." → Use attribute name "BMI".

## Step 3: Determine the value expression

The value is sent to the server as a string.

- Literal text values must be wrapped in double quotes, e.g. `"diabetic"`.
- Numeric values and formulas over attribute names are sent unquoted, e.g. `7` or `systolic - diastolic`.
- Send the expression **as the user wrote it**. Do not correct the spelling of an attribute name, do not rewrite an
  operator (`height ^ 2` must not become `height * height`), and never substitute a formula appearing in these
  instructions for the one the user gave. The server resolves each name against the knowledge base and asks the user
  about any it cannot resolve, so a name you silently fix is a change the user is never shown, and an expression you
  tidy is not the one they asked for.
- There is one exception, and only one. When the server has asked "Did you mean ...?" and the user accepts, send the
  corrected expression **from the server's question**, not the words the user originally typed. Re-sending the original
  would fetch the same question again and the two of you would loop. The correction is allowed here because the server
  proposed it and the user agreed to it in so many words; it is not you deciding what they meant.
- The server decides whether the expression is a literal, number, or formula, so your job is only to apply the quoting
  rule above.

## Step 4: Emit the JSON action

### To create and assign a new derived value

```json
{
  "action": "{{ASSIGN_DERIVED_VALUE}}",
  "attributeName": "Diabetes status",
  "valueExpression": "\"diabetic\""
}
```

For a formula, where the user asked to calculate pulse pressure as `systolic - diastolic`:

```json
{
  "action": "{{ASSIGN_DERIVED_VALUE}}",
  "attributeName": "Pulse pressure",
  "valueExpression": "systolic - diastolic"
}
```

### To remove a derived value

```json
{
  "action": "{{REMOVE_DERIVED_VALUE}}",
  "attributeName": "Diabetes status"
}
```

### To replace a derived value

```json
{
  "action": "{{REPLACE_DERIVED_VALUE}}",
  "attributeName": "Diabetes status",
  "valueExpression": "\"severely diabetic\""
}
```

These actions start a rule session. After starting, present suggested conditions and follow the normal rule-building
flow ("Defining the reasons for report change" and cornerstone handling).

## Step 5: Handle server refusals

The server may refuse the request for one of three reasons:

- The attribute name is already used by another derived attribute (case-insensitive). Relay the server's message
  verbatim and ask the user to pick a different name.
- The attribute name is already used by an externally supplied case attribute. Relay the server's message verbatim and
  ask the user to pick a different name.
- The value expression would create a dependency cycle (e.g. assigning BMI the value `BMI * 2`). Relay the message
  verbatim and ask the user to correct the expression.

The server may also answer with a *question* rather than a refusal, when a name in the expression is no attribute:
either
"Did you mean ...?", naming a correction, or "Do you want to assign the text ...?". Relay it verbatim. If the user
accepts, emit the action again with the expression exactly as the server wrote it in the question — quoted, if it
offered the text. If the user declines, ask what they meant instead.

## Step 6: Cornerstone cases

After emitting the action, you will receive a cornerstone status. If there are cornerstone cases, follow the existing
cornerstone review flow before committing the rule.
