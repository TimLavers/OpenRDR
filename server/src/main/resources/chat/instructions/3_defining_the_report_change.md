# Defining the change to the report

The current report for the case is a list of comments. For the current case, the list of comments is as follows:

{{COMMENTS}}

## Step 1: Ask the user if they want to change the report for the case:

- If there are no comments, ask the user if they want to add a comment.
- Else, if there is at least one comment, display the comments with an index and ask the user if they want to add,
  remove, or replace a comment.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about changing the report>"
}
```

## Step 2: Ask the user what type of change they want to make:

- For add: Collect the comment text.
- For remove: If there is only one comment, use that comment directly without asking the user which one. If there are
  multiple comments, ask which comment to remove (e.g., by index or text).
- For replace: If there is only one comment, use that comment as the one to replace without asking which one. If there
  are multiple comments, ask which comment to replace. Then collect the replacement text.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about the type of change they want to make>"
}
```

## Step 3. Ask for confirmation for the user-requested change:

- If the user specifies a comment ending in a period, for example, "Go to Bondi.", do not remove the period from the end
  of the comment.
- If the user has already provided the exact comment text in quotes (e.g., Add the comment: "Beach time!"), skip
  confirmation and proceed directly to Step 4 by outputting the ADD_COMMENT action immediately. DO NOT call any
  transform functions - the quoted text is the comment to be added, not a reason to be transformed.
- Otherwise, summarize the report change and ask the user to confirm.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<summary of the report change and request for confirmation>"
}
```

## Step 4: Inform the system to start a rule session:

- Once the user confirms the report change (or if confirmation was skipped because the comment was quoted),
  follow the instructions "Starting the rule session".