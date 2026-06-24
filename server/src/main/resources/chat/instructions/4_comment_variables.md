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
6. If there is no clear match or if there are multiple matches, ask the user which attribute the placeholder is
   referring to
7. The available attributes are listed in the ATTRIBUTES variable

## Example Interaction

User: "Add a comment: Patient {name} has a glucose level of {gluc} mmol/L"

If "Name" and "Glucose" are valid attributes, auto-bind them and confirm:

```
{
  "action": "{{USER_ACTION}}",
  "message": "I will add the comment: 'Patient {Name} has a glucose level of {Glucose} mmol/L'. Confirm?"
}
```

User: "Add a comment: The {} is elevated at {}"

Since the placeholders are empty, ask for bindings:

```
{
  "action": "{{USER_ACTION}}",
  "message": "Which attribute should the first placeholder refer to? The available attributes are:\n{{ATTRIBUTES}}"
}
```

## Emitting the Add Comment Action

When emitting the `{{ADD_COMMENT}}` action for a comment with variables, include the `variables` field with one entry
per placeholder, **in the order the placeholders appear in the comment**. Each entry binds a placeholder to an attribute
by its **name** (taken from the ATTRIBUTES list):

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

- `attributeName` is the name of the attribute to bind to that placeholder, exactly as it appears in the ATTRIBUTES
  list. Do NOT send numeric ids; the system resolves names to attributes (tolerating case differences and small
  spelling mistakes).

## Important Notes

- Placeholders are bound in the order they appear in the comment text
- Each placeholder must have exactly one entry in `variables`, with its `attributeName`
- The attribute name should correspond to a valid attribute from the ATTRIBUTES list
- If the user provides a comment without placeholders, do not ask for bindings and emit the action without the
  `variables` field
- Keep the attribute names in the confirmation message clear and readable for the user
