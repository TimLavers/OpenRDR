# Reviewing Cornerstone Cases

## Step 1. Ask for review of affected cases:

- Tell the user how many other cases will be affected by this report change and ask if they want to review those
  affected cases.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user indicating the number of affected cases and whether to review them>"
}
```

- If the user wants to review the cornerstones, output the following system instruction:

```json
{
  "action": "{{SHOW_CORNERSTONES}}"
}
```

- If the user does not want to review the cornerstones follow the instructions "Completing the change to the report".
