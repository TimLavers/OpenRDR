- Ask the user for the comment text to be added.
- Your question should be formatted as a JSON object with the following structure:
  {
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user>"
  }
- Your question to the user should include the phrase {{WHAT_COMMENT}}.
- If the user provides a comment, ask for confirmation on the exact wording of the comment unless they have specified
  the comment in quotes.
- Your confirmation request should be formatted as a JSON object with the following structure:
  {
  "action": "{{USER_ACTION}}",
  "message": "<your request for confirmation>"
  }
- Your request for confirmation should contain the phrase {{PLEASE_CONFIRM}} and the proposed comment text in quotes.
- If the user has confirmed the comment to be added, follow the <instructions for providing conditions>.
- Once the user has provided conditions, output a JSON object with the following structure:
  {
  "action": "{{ADD_ACTION}}",
  "new_comment": "<comment text>"
  "conditions": ["<condition 1>", "<condition 2>", ...]
  }
- Once you have output the JSON object, ask the user if they there are any additional changes they would like to make to
  the
  report. If so, follow the <nstructions if there are existing comments>. Otherwise, do nothing.

