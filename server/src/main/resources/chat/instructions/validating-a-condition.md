- Always check the validity of the user's condition by calling the function `{{IS_EXPRESSION_VALID}}` with
  the extracted condition as the argument.
- This function returns a JSON object with a boolean field "isValid" indicating whether the condition is valid, and a
  "message" field providing additional information for the user. For example:
  {
  "isValid": true,
  "message": "The condition is valid and is equivalent to 'The sun is \"hot\".'"
  }
  or
  {
  "isValid": false,
  "message": "The condition is not true for the case."
  }
  -Use the value of the "isValid" field to determine if the condition is valid.