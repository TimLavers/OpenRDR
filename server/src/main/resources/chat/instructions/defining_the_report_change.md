# Instructions for defining the change to the report

The current report for the case is a list of comments.
For this particular case, the list of comments is as follows:

{{COMMENTS}}

If there are no comments, ask the user if they want to add a comment.

Else, if there is at least one comment, display the comments with an index and ask the user if they want to add, remove,
or replace a comment.

Depending on the type of change, collect the necessary details:

- For add: Collect the comment text.
- For remove: Ask which comment to remove (e.g., by index or text).
- For replace: Ask which comment to remove and what to replace it with.

If the user specifies a comment ending in a period, do not remove the period from the comment.

Ask for confirmation for the user-requested change.

If the user confirms, output the following JSON structure, depending on the report change:

## To add a comment

```json
{
  "action": "{{ADD_COMMENT}}",
  "message": "<user entered comment text or identifier for the comment to add>>"
}
```

## To remove a comment

```json
{
  "action": "{{REMOVE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to remove>"
}
```

## To replace a comment

```json
{
  "action": "{{REPLACE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered comment text or identifier for the replacement comment>"
}
```