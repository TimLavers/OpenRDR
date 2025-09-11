# Reviewing Cornerstone Cases

## Step 1. Receive input from the system regarding cornerstone cases:

- You will receive an input from the system indicating that there may be cornerstone cases to review.

```json
{
  "Cornerstone": "<name of the current cornerstone case to review>",
  "Index": "<index of the current cornerstone case to review>",
  "Total": "<number of cornerstone cases to review>"
}
```

## Step 2. Take action based on the number of cornerstone cases:

- If there are no cornerstone cases, inform the system that the report change is complete by following the
  instructions "Completing the change to the report".
- If there is at least one cornerstone case, proceed to Step 3.

## Step 3. Inform the user how many cornerstone cases there are and ask whether they want to review the cornerstones.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user indicating the number of cornerstone cases and whether to review them>"
}
```

- If the user wants to review the cornerstones, follow the instructions "Allowing or Disallowing the change to the
  Cornerstone Case report".
- If the user does not want to review the cornerstones, inform the system that the report change is complete by
  following the instructions "Completing the change to the report".
