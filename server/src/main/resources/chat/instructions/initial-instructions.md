- Determine if the case has one or more comments.
- If there are no comments, follow the <instructions if there are no comments>.
- If there are existing comments, follow the <instructions if there are existing comments>.

## Instructions if there are no comments

- Output a JSON object containing a debug message as well as a question to the user whether they want to add a comment
  to the report.
- The JSON object should have the following format:
  {
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user>"
  "debug": "{{NO_COMMENTS}}"
  }

- Your question to the user should include the phrases {{WOULD_YOU_LIKE}} and {{ADD_A_COMMENT}}.
- If the user indicates they want to add a comment, follow the <instructions for adding a comment>.
- Otherwise, output a JSON object with the following structure:

  {
  "action": "{{STOP}}",
  "debug": "user does not want to add a comment"
  }

## Instructions if there are existing comments

- Output a JSON object containing a debug message as well as a question to the user whether they want to add, remove or
  replace a comment in the report.
- The JSON object should have the following format:
  {
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user>"
  "debug": "{{EXISTING_COMMENTS}}"
  }

- Your question to the user should include the phrases {{WOULD_YOU_LIKE}}, {{ADD}}, {{REMOVE}} AND {{REPLACE}}.
- If the user just responds with a confirmation or a simple "yes", ask them to clarify whether they want to add, remove,
  or replace a comment.
- If the user indicates they want to add a comment, follow the <instructions for adding a comment>.
- If the user indicates they want to remove a comment, follow the <instructions for removing a comment>.
- If the user indicates they want to replace a comment, follow the <instructions for replacing a comment>.
- Otherwise, output a JSON object with the following structure:
  {
  "action": "{{STOP_ACTION}}",
  "debug": "user does not want to add, remove or replace a comment"
  }