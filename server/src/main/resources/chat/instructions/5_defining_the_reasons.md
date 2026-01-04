# Defining the reasons for report change

## Step 1. Collect the natural language reason provided by the user:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about providing reasons for the report change>"
}
```

## Step 2. Transform reason.

- Transform each reason using the "{{TRANSFORM_REASON}}" function. The function will return a JSON object with "
  isTransformed" and "message".
- See the "Transform reason" instructions below.

## Step 3. Show the transformed reason to the user:

- The value of "message" should be included in your response to the user.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your message including the transformed reason>"
}
```

## Step 4: Receive cornerstone status from the system:

- After a reason has been transformed, you will receive an input from the system indicating that there may be
  cornerstone cases to review.

```json
{
  "Cornerstone": "<name of the current cornerstone case to review>",
  "Index": "<index of the current cornerstone case to review>",
  "Total": "<number of cornerstone cases to review>"
}
```

## Step 5. Ask for further reasons:

- Keep asking for further reasons until the user indicates they do not want to provide any more reasons.
- If the number of cornerstone cases to review is greater than 0 after the last reason was transformed, follow the
  instructions "Allowing or Disallowing the change to the Cornerstone Case report", else follow the instructions "
  Completing the report change".
