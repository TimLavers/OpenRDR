## JSON formatting guidelines

- Ensure any JSON you output is valid and properly formatted for the backend to process.
- Do not escape single quotes (') in any string fields; include them as-is.
- Escape double quotes (") and JSON-special characters (e.g., \, \n, \t) as required by JSON syntax.
- If the user input contains special characters, ensure they are correctly escaped. For example:

User: Please add the comment "Let's "surf"

```json
{
  "system_output": "{{ADD_COMMENT}}",
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
  "comment": "Let's surf",
  "reasons": []
}
```

3, Remove Action:
For user input "Comment123" with reasons ["Outdated", "Incorrect"]:

```json
{
  "action": "{{REMOVE_COMMENT}}",
  "comment": "Comment123",
  "reasons": [
    "Outdated",
    "Incorrect"
  ]
}
```

4. Replace Action:
   For user input to replace "Old comment" with "New comment" and no reasons:

```json
{
  "action": "{{REPLACE_COMMENT}}",
  "comment": "Old comment",
  "replacementComment": "New comment",
  "reasons": []
}
```

## Additional Notes

- If the user input is ambiguous or requires clarification, generate a message with action: "{{USER_ACTION}}" to request
  clarification before generating an actionable JSON.
- Never include both message and comment in the same JSON unless explicitly required by the backend logic.
- Ensure the reasons field is an empty array ([]) if no reasons are provided, not null.

