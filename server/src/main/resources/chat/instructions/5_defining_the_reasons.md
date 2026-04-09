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
  reasonId" and "message".
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
  cornerstone cases to review. The format is:

```
Cornerstone: <name of the current cornerstone case, or null>, Index: <index>, Total: <number of cornerstone cases>
```

## Step 5. Ask for further reasons:

- After processing each reason (and after handling any cornerstone cases if applicable), you MUST call
  {{GET_SUGGESTED_CONDITIONS}} to refresh the list of suggested conditions, present the updated suggestions to the user,
  and ask if they would like to provide another reason. Your message MUST include the word "reason" (e.g.
  "Do you want to provide any more reasons?"). Do this regardless of the cornerstone count.
- If the number of cornerstone cases to review is greater than 0 after the last reason was transformed, follow the
  instructions "Allowing or Disallowing the change to the Cornerstone Case report" BEFORE asking for further reasons.
- Keep asking for further reasons until the user explicitly indicates they do not want to provide any more reasons.
- ONLY after the user explicitly indicates they do not want to provide any more reasons, follow the instructions
  "Completing the report change". Do NOT commit the rule until the user has explicitly declined to add more reasons.

## Step 6. Allow the user to remove a reason:

- If the user indicates that a reason be removed, output the following with the exact condition text:

```json
{
  "action": "{{REMOVE_REASON}}",
  "comment": "Sun is \"hot\""
}
```

- After a reason has been removed, you will receive an input from the system indicating that there may be
  cornerstone cases to review as per Step 4.
- Inform the user that the reason has been removed and ask if they want to provide more reasons. Your
  message MUST include the word "reason". Do NOT jump straight to showing suggestions — first confirm the
  removal and ask about more reasons. For example:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "The reason has been removed. Do you want to provide any more reasons?"
}
```

## Step 7. Allow the user to see all reasons:

- If the user asks to see the reasons entered so far, list them to the user as a numbered list with one reason per line,
  for example:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "1. <first reason>\\n2. <second reason>\\n3. <third reason>"
}
```


