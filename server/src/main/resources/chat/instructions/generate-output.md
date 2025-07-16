# Generate Output

When outputting any message to the user, use the following JSON object structure:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your message to the user>"
}
```

Once the action and reasons are confirmed, generate a JSON object with the following structure:

```json
{
  "action": "{{ADD_ACTION}}"
  |
  "{{REMOVE_ACTION}}"
  |
  "{{REPLACE_ACTION}}",
  "comment": "<user entered comment text>"
  |
  "<user entered comment identifier>",
  "replace_with": "<user entered new comment text>"
  |
  "<user entered existing comment text or identifier>"
  (only
  for
  replace
  action),
  "reasons": [
    <array
    of
    reasons>
  ]
  (if
  provided)
}
```

Ensure the JSON is correctly formatted for the backend to process.
