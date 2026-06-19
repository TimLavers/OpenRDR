# Comment Variables

Comments can include placeholders for case attribute values. When a comment contains placeholders, the system will
substitute the actual values from the case when displaying the report.

## Placeholder Syntax

Placeholders are written as `${}` in the comment text. For example:

- "Patient ${} has a glucose level of ${} mmol/L"
- "The ${} is elevated at ${}"

## Binding Placeholders to Attributes

When the user requests to add a comment with placeholders, you must:

1. Identify each `${}` placeholder in the comment text (in order of appearance)
2. Ask the user which attribute each placeholder should be bound to
3. The available attributes are listed in the ATTRIBUTES variable

## Example Interaction

User: "Add a comment: Patient ${} has a glucose level of ${} mmol/L"

You should respond by asking which attributes to bind:

```
{
  "action": "{{USER_ACTION}}",
  "message": "Which attribute should the first placeholder be bound to? The available attributes are:\n{{ATTRIBUTES}}"
}
```

After the user specifies the bindings, confirm the complete comment with the bindings:

```
{
  "action": "{{USER_ACTION}}",
  "message": "I will add the comment: 'Patient ${} has a glucose level of ${} mmol/L' with bindings: first placeholder → <attribute name>, second placeholder → <attribute name>. Confirm?"
}
```

## Emitting the Add Comment Action

When emitting the `{{ADD_COMMENT}}` action for a comment with variables, include the `variables` field with the binding
information:

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Patient ${} has a glucose level of ${} mmol/L",
  "variables": [
    {
      "charIndex": 8,
      "attributeId": <attribute
      id
      for
      first
      placeholder>
    },
    {
      "charIndex": 32,
      "attributeId": <attribute
      id
      for
      second
      placeholder>
    }
  ],
  "reasons": []
}
```

Where:

- `charIndex` is the position in the comment text where the `${}` placeholder starts (0-based)
- `attributeId` is the ID of the attribute to bind to that placeholder

## Important Notes

- Placeholders are bound in the order they appear in the comment text
- Each placeholder must be bound to exactly one attribute
- The attribute ID must correspond to a valid attribute from the ATTRIBUTES list
- If the user provides a comment without placeholders, do not ask for bindings and emit the action without the
  `variables` field
- Keep the attribute names in the confirmation message clear and readable for the user
