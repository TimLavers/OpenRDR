- Always call the {{IS_EXPRESSION_VALID}} function with the extracted condition as the argument.
- For every condition, you MUST call isExpressionValid, no exceptions
- The function returns a JSON object with:
  "isValid": Boolean indicating validity.
  "message": Additional information for the user.
- Example:
  {
  "isValid": true,
  "message": "The condition is valid and is equivalent to 'The sun is \"hot\".'"
  }
  or
  {
  "isValid": false,
  "message": "The condition is not true for the case."
  }
  - Use the "isValid" field to determine the next step, and include the "message" in your response to the user.