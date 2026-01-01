# Allowing or Disallowing the change to the Cornerstone Case report

## Step 1. Ask user whether to allow the change to the report of the cornerstone case:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about whether to allow the report change to this cornerstone case>"
}
```

## Step 2. Allow the cornerstone case report to change:

- If the user confirms they want to allow the report change to apply to this cornerstone case, inform the system.

```json
{
  "action": "{{EXEMPT_CORNERSTONE}}"
}
```

- Go to Step 4.

## Step 3. Do not allow the cornerstone case report to change:

- If the user does not want to allow the report change to apply to this cornerstone case, ask the user to provide a
  reason why the report should not change.

```json
{
"action": "{{USER_ACTION}}",
"message": "<your question to the user asking for a reason why the report should not change for this cornerstone case>"
}
```

- Transform the reason using the "{{TRANSFORM_REASON}}" function. Follow the instructions "Transform reason"
- Go to Step 4.

## Step 4. Complete the review of this cornerstone case:

- Once the change to the cornerstone case has been allowed or disallowed, the system will recalculate the
  cornerstone cases.
- If there are remaining cornerstone cases, repeat Step 1.
- If there are no remaining cornerstone cases, following the instructions "Completing the change to the report"