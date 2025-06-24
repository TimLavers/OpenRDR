- Ask the user if there are any conditions for this change to the report.
- Your question should be formatted as a JSON object with the following structure:
  {
  "action": "{{USER_ACTION}}",
  "message": <your question for a condition>
  }
- Your question should include the phrase {{ANY_CONDITIONS}}
- If the user indicates there are no conditions (e.g., "no"), proceed to output the final JSON object for the action
  without conditions.
- If the user confirms there are conditions (e.g. "yes"). Then do the following:
    - Ask the user to provide a condition.
    - Extract the condition from the user's message.
        - If the condition is enclosed in quotes, extract the text within the quotes.
        - If the condition is prefixed with "add the condition", extract the text after this phrase.
        - If the condition is a simple statement, use it as is.
- Validate the condition by following the <instructions for validating the condition>
- Determine whether the condition is valid from the value of the "isValid" field in the json result of this function
  call.
    - If the condition is valid your response should be the following JSON object:
      {
      "action": "{{USER_ACTION}}",
      "message": <message returned by the function call>. Do you have any more conditions?
      }
    - If the condition is not valid, your response should be the following JSON object:
      {
      "action": "{{USER_ACTION}}",
      "message": <message returned by the function call>. Please rephrase the condition.
      }
    - Example of an invalid condition:
        - User message: "add the condition 'The sun is hot.'"
        - Response: [Function call: {{IS_EXPRESSION_VALID}}("The sun is hot.")] which returns:
          {
          "isValid": false,
          "message": "The condition is not true for the case."
          }
        - Response to user:
          {
          "action": "{{USER_ACTION}}",
          "message": "The condition is not true for the case. Please rephrase the condition."
          }
    - Example of a valid condition:
    - User message: "add the condition 'The sun is hot.'"
    - Response: [Function call: {{IS_EXPRESSION_VALID}}("The sun is hot.")] which returns:
      {
      "isValid": true,
      "message": "The condition is valid and is equivalent to 'The sun is \"hot\".'"
      }
    - Response to user:
      {
      "action": "{{USER_ACTION}}",
      "message": "The condition is valid and is equivalent to 'The sun is \"hot\".' Do you have any more conditions?"
      }
    - If no condition is provided (e.g., the user just says "yes"), ask for the first condition:
      {
      "action": "{{USER_ACTION}}",
      "message": <your question for a condition>"
      }
    - Your question should include the phrase "{{FIRST_CONDITION}}".
- After receiving a condition and generating the function call:
    - If the function returns that the condition is valid, store it in a list of validated conditions.
    - Ask the user if there are any more conditions:
      {
      "action": "{{USER_ACTION}}",
      "message": <your question for more conditions>
      "debug": <the last condition validated>
      }
    - Your question should include the phrase "{{ANY_MORE_CONDITIONS}}".
- Continue asking for conditions until the user indicates there are no more conditions (e.g., "no"
  to <your question for more conditions>).
- Once all conditions are collected and validated, output the final JSON object for the action (add, remove, or replace)
  with the stored and valid conditions in the "conditions" array.
