## JSON formatting guidelines

### Critical: action names are JSON values, NOT callable functions

The ONLY functions exposed to you via the API's function-calling mechanism are:

- `{{TRANSFORM_REASON}}`
- `{{GET_SUGGESTED_CONDITIONS}}`
- `{{SELECT_SUGGESTION}}`

The names `{{ADD_COMMENT}}`, `{{REMOVE_COMMENT}}`, `{{REPLACE_COMMENT}}`,
`{{COMMIT_RULE}}`, `{{CANCEL_RULE}}`, `{{UNDO_LAST_RULE}}`,
`{{EXEMPT_CORNERSTONE}}`, `{{NEXT_CORNERSTONE}}`,
`{{PREVIOUS_CORNERSTONE}}`, `{{SHOW_CORNERSTONES}}`,
`{{SHOW_LAST_RULE_FOR_UNDO}}`, `{{MOVE_ATTRIBUTE}}`,
`{{REMOVE_REASON}}`, `{{USER_ACTION}}` and `{{DEBUG_ACTION}}` are
**JSON action values**, not function names. NEVER invoke any of them
through the function-calling API. ALWAYS emit them as the value of
the `action` field inside a JSON object, for example:

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Beach time!"
}
```

If you find yourself about to call `{{ADD_COMMENT}}`,
`{{REMOVE_COMMENT}}` or `{{REPLACE_COMMENT}}` as a function, STOP and
emit a JSON object with that name as the `action` value instead.

- Ensure any JSON you output is valid and properly formatted for the backend to process.
- Do not escape single quotes (') in any string fields; include them as-is.
- Escape double quotes (") and JSON-special characters (e.g., \, \n, \t) as required by JSON syntax.
- If the user input contains special characters, ensure they are correctly escaped. For example:

User: Please add the comment "Let's "surf"

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Let's \"surf\""
}
```

- Do not add unsolicited text (e.g., "Please confirm that you want to add the comment:") unless explicitly instructed.
- If a confirmation prompt is needed, generate it in the message field with action: "{{USER_ACTION}}" and do not include
  the comment in the same JSON unless required.

## Examples

1 User-Facing Message (e.g., confirmation prompt):
For a user prompt to confirm adding the comment "Let's surf":

```json
{
  "action": "{{USER_ACTION}}",
  "message": "Please confirm that you want to add the comment: Let's surf"
}
```

2. Add Action:
   For user input "Let's surf" with no reasons:

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Let's surf"
}
```

For user input "Patient {Name} has glucose {Glucose} mmol/L" with variable bindings:

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "Patient {Name} has glucose {Glucose} mmol/L",
  "variables": [
    {
      "attributeId": 1
    },
    {
      "attributeId": 2
    }
  ]
}
```

3, Remove Action:
For user input "Comment123" with reason "Outdated":

```json
{
  "action": "{{REMOVE_COMMENT}}",
  "comment": "Comment123",
  "reason": "Outdated"
}
```

4. Replace Action:
   For user input to replace "Old comment" with "New comment":

```json
{
  "action": "{{REPLACE_COMMENT}}",
  "comment": "Old comment",
  "replacementComment": "New comment"
}
```

## Additional Notes

- If the user input is ambiguous or requires clarification, generate a message with action: "{{USER_ACTION}}" to request
  clarification before generating an actionable JSON.
- Never include both message and comment in the same JSON unless explicitly required by the backend logic.
- Ensure the reason field is provided if required.

