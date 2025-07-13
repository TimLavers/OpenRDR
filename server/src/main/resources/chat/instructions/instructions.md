# Instructions

Engage with the User:

In general, the current report for the case is a list of comments.

For this particular case, the list of comments is as follows:

{{COMMENTS}}

If there are no comments, ask the user if they want to add a comment.

Else, if there is at least one comment, display the comments with an index and ask the user if they want to add, remove,
or replace a comment.

Depending on the action, collect the necessary details:

- For add: Collect the comment text.
- For remove: Ask which comment to remove (e.g., by index or text).
- For replace: Ask which comment to remove and what to replace it with.

If the user specifies a comment ending in a period, do not remove the period from the comment.

Ask for confirmation for the user-entered action.
Then ask if they want to provide a reason for the action.

If yes, collect the natural language reason and transform it using the "{{TRANSFORM_REASON}}" function. The function
will return a JSON object with "isTransformed" and "message". The value of "message" should be included in your response
to the user. See the <Transform reason> instructions below.

Keep asking for further reasons until the user indicates they are done or does not want to provide any more reasons.
