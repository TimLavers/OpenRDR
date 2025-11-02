# Completing the change to the report

## Step 1: Inform the system of the report change:

- your response to the system should be one of the following, depending on the report change:

### To add a comment

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to add>>",
  "reasons": [
    "<list of reasons provided by the user, if any>"
  ]
}
```

### To remove a comment

```json
{
  "action": "{{REMOVE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to remove>",
  "reasons": [
    "<list of reasons provided by the user, if any>"
  ]
}
```

### To replace a comment

```json
{
  "action": "{{REPLACE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered comment text or identifier for the replacement comment>",
  "reasons": [
    "<list of reasons provided by the user, if any>"
  ]
}
```

## Step 2: Inform the user of the completed report change:

- After informing the system of the report change, inform the user that the report change has been completed.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your message to the user confirming that the report change has been completed>"
}
```
