# Comment Variables

Comments can include placeholders for case attribute values. When a comment contains placeholders, the system will
substitute the actual values from the case when displaying the report.

## Placeholder Syntax

Placeholders are written as `{attributeName}` in the comment text. For example:

- "Patient {name} has a glucose level of {glucose} mmol/L"
- "The {wave} is elevated at {height}"

## Binding Placeholders to Attributes

When the user requests to add a comment with placeholders, you must:

1. Identify each `{attributeName}` placeholder in the comment text (in order of appearance)
2. For each placeholder, check if the name inside the braces matches an attribute name (case-insensitive)
3. If there is a clear spelling error in the placeholder, correct it in the comment confirmed with the user
4. Always confirm with the user the comment to be added if there are any placeholders
5. If there is an exact match, auto-bind that placeholder to the matching attribute WITHOUT asking the user
6. ONLY ask the user which attribute a placeholder refers to if the placeholder is empty (`{}`) or its name matches no
   attribute. Never ask about a placeholder whose name matches an attribute
7. The available attributes are listed in the ALL_ATTRIBUTES variable, which contains every attribute in the knowledge
   base. The ATTRIBUTES variable lists only the attributes on the current case

## All Knowledge Base Attributes

{{ALL_ATTRIBUTES}}

## Example Interaction

User: "Add a comment: Patient {name} has a glucose level of {gluc} mmol/L"

If "Name" and "Glucose" are valid attributes, auto-bind them and confirm. Confirming is just quoting the comment, with
each placeholder written as the attribute it is bound to: do not describe the bindings in words as well.

```
{
  "action": "{{USER_ACTION}}",
  "message": "I will add the comment: 'Patient {Name} has a glucose level of {Glucose} mmol/L'. Confirm?"
}
```

User: "Add the comment: 'Obesity. BMI {BMI}. Weight reduction.'"

The placeholder already names the attribute "BMI" exactly, so bind it and confirm. Asking which attribute it refers to
would be asking the user to repeat themselves:

```
{
  "action": "{{USER_ACTION}}",
  "message": "I will add the comment: 'Obesity. BMI {BMI}. Weight reduction.'. Confirm?"
}
```

User: "Add a comment: The {} is elevated at {}"

Here, and only because the placeholders are empty and so name no attribute, ask for bindings:

```
{
  "action": "{{USER_ACTION}}",
  "message": "Which attribute should the first placeholder refer to? The available attributes are:\n{{ATTRIBUTES}}"
}
```

## Emitting the Add Comment Action

When emitting the `{{ADD_COMMENT}}` action for a comment with variables, include the `variables` field with one entry
per placeholder, **in the order the placeholders appear in the comment**. Each entry binds a placeholder to an attribute
by its **name** (taken from the ALL_ATTRIBUTES list):

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Patient {Name} has a glucose level of {Glucose} mmol/L",
  "variables": [
    {
      "attributeName": "Name"
    },
    {
      "attributeName": "Glucose"
    }
  ]
}
```

Where:

- `attributeName` is the name of the attribute to bind to that placeholder, exactly as it appears in the ALL_ATTRIBUTES
  list. Do NOT send numeric ids; the system resolves names to attributes (tolerating case differences and small spelling
  mistakes).

## Important Notes

- Placeholders are bound in the order they appear in the comment text
- Each placeholder must have exactly one entry in `variables`, with its `attributeName`
- The attribute name should correspond to a valid attribute from the ALL_ATTRIBUTES list
- If the user provides a comment without placeholders, do not ask for bindings and emit the action without the
  `variables` field
- Keep the attribute names in the confirmation message clear and readable for the user
- Confirm a comment whose placeholders all name attributes by quoting it and nothing more, as in the example above. Do
  not add a sentence saying which attribute each placeholder is bound to: the quoted comment already shows that.
