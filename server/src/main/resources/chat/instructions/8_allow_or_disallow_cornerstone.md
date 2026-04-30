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

- Go to Step 5.

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
- Go to Step 5.

## Step 4. Navigate between cornerstone cases:

- If the user asks to see the next cornerstone case, navigate to it:

```json
{
  "action": "{{NEXT_CORNERSTONE}}"
}
```

- If the user asks to see the previous cornerstone case, navigate to it:

```json
{
  "action": "{{PREVIOUS_CORNERSTONE}}"
}
```

- After navigating, the system will send an updated cornerstone status message. Repeat Step 1 for the new cornerstone
  case.

## Step 5. Complete the review of this cornerstone case:

- Once the change to the cornerstone case has been allowed or disallowed, the system will recalculate the
  cornerstone cases and send you a message in the format "Cornerstone: ..., Index: ..., Total: ...".
- **IMPORTANT: Always use the MOST RECENT cornerstone status.** Earlier conversation turns may show a
  different Total — ignore those. The authoritative source, in order of preference, is:
  1. the `[Current cornerstone status: ...]` prefix attached to the latest user message;
  2. the cornerstone status returned by the most recent {{TRANSFORM_REASON}}, {{SELECT_SUGGESTION}},
     {{EXEMPT_CORNERSTONE}}, {{NEXT_CORNERSTONE}} or {{PREVIOUS_CORNERSTONE}} call.
     If the most recent Total > 0, there are remaining cornerstone cases and you MUST repeat Step 1 for each one.
     Do NOT skip any cornerstone cases. Do NOT commit the rule while there are remaining cornerstone cases.
- Only after ALL cornerstone cases have been reviewed (Total is 0):
  - If the user has already declined to provide reasons at any earlier point in the conversation
    (e.g. they said "no" when presented with suggestions, or said "no" when asked if they want to provide
    more reasons), you MUST immediately respond with:
    ```json
    {
      "action": "{{COMMIT_RULE}}"
    }
    ```
    Do NOT ask for more reasons. Do NOT call {{GET_SUGGESTED_CONDITIONS}}. Commit immediately.
  - If the user has NOT yet been asked about reasons or has not yet declined, follow the instructions in
    "Defining the reasons for report change" Step 5 to ask for further reasons.