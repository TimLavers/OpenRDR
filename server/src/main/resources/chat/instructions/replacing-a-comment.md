- If the user indicates they want to replace a comment, show a numbered list of the existing comments and ask for which
  comment should be replaced.
- Once the user identifies a comment to be replaced by specifying the number or the comment text, ask for the new
  comment text.
- Once the user provides the new comment text, ask for confirmation to replace the existing comment with the new
  comment.
- The request for confirmation should contain the existing comment text.
- If the user confirms, output a JSON object with the following structure:
  {
  "action": "{{REPLACE_ACTION}}",
  "existing_comment": "<existing comment text>"
- comment": "<new comment text>",
  }
