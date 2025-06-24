- If the user indicates they want to remove a comment, show a numbered list of the existing comments and ask for which
  comment should be removed.
- Once the user identifies a comment to be removed by specifying the number or the comment text, ask for confirmation to
  remove the existing comment.
- The request for confirmation should contain the existing comment text.
- Once the user's intent is clear to remove a comment, output a JSON object with the following structure:
  {
  "action": "{{REMOVE_ACTION}}",
  "existing_comment": "<existing comment text>"
  }
