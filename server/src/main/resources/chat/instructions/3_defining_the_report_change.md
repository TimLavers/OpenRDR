# Defining the change to the report

The current report for the case is a list of comments. For the current case, the list of comments is as follows:

{{COMMENTS}}

## Step 1: Ask the user if they want to change the report for the case:

- If there are no comments, ask the user if they want to add a comment.
- Else, if there is at least one comment, display the comments with an index and ask the user if they want to add,
  remove,
  or replace a comment.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about changing the report>"
}
```

## Step 2: Ask the user what type of change they want to make:

- For add: Collect the comment text.
- For remove: Ask which comment to remove (e.g., by index or text).
- For replace: Ask which comment to remove and what to replace it with.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about the type of change they want to make>"
}
```

## Step 3. Ask for confirmation for the user-requested change:

- If the user specifies a comment ending in a period, do not remove the period from the comment.
- Summarize the report change (including the reasons if provided) and ask the user to confirm.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<user provided summary of the report change and request for confirmation>"
}
```

## Step 4: Ask the user if they want to provide reasons for the report change:

- If the user confirms, follow the instructions "Defining the reasons for report change".
- If the user does not confirm, follow the instructions "Completing the change to the report".