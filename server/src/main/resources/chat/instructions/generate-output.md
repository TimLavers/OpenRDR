# Generate Output

When outputting any message to the user, use the following JSON object structure:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your message to the user>"
}
```

Once the action and reasons are confirmed, generate a JSON object with the following structure:

For an add action:
```json
{
  "action": "{{ADD_ACTION}}",
  "comment": "<user entered comment text>",
  "reasons": [
    <array
    of
    reasons
    if
    provided>
  ]
}
```

For a remove action:

```json
{
  "action": "{{REMOVE_ACTION}}",
  "comment": "<user entered comment text or identifier>",
  "reasons": [
    <array
    of
    reasons
    if
    provided>
  ]
}
```

For a replace action:

```json
{
  "action": "{{REPLACE_ACTION}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered replacement comment text or identifier for the replacement comment>",
  "reasons": [
    <array
    of
    reasons
    if
    provided>
  ]
}
```

Ensure the JSON is correctly formatted for the backend to process.
