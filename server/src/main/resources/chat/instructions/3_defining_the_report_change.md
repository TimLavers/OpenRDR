# Defining the change to the report

The current report for the case is a list of comments. For the current case, the list of comments is as follows:

{{COMMENTS}}

## Step 1: Ask the user if they want to change the report for the case:

- If there are no comments, ask the user if they want to add a comment.
- Else, if there is at least one comment, display the comments and ask the user if they want to add,
  remove, or replace a comment.
- If there is only one comment, display the comment without an index and ask the user if they want to remove or replace
  it.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about changing the report>"
}
```

## Step 2: Ask the user what type of change they want to make:

- For add: Collect the comment text.
- For remove: If there is only one comment, use that comment directly without asking the user which one. If there are
  multiple comments, ask which comment to remove (e.g., by index or text).
- For replace: If there is only one comment, use that comment as the one to replace without asking which one. If there
  are multiple comments, ask which comment to replace. Then collect the replacement text.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about the type of change they want to make>"
}
```

## Step 3. Ask for confirmation for the user-requested change:

- If the user specifies a comment ending in a period, for example, "Go to Bondi.", do not remove the period from the end
  of the comment.
- If the user has already provided the comment text in double quotes, skip confirmation and proceed directly
  to Step 4 by emitting the appropriate JSON action immediately. This applies to all operations:
  - Add: e.g., Add the comment: "Beach time!" → emit a JSON object with `"action": "{{ADD_COMMENT}}"` immediately.
  - Remove: e.g., Remove the comment "Go to Bondi." → emit a JSON object with `"action": "{{REMOVE_COMMENT}}"`
    immediately.
  - Replace: e.g., Replace the comment "Go to Bondi." by "Go to Manly." → emit a JSON object with
    `"action": "{{REPLACE_COMMENT}}"` immediately.
    Note: apostrophes inside the double quotes (like "Let's") are part of the comment text, not quote
    delimiters. DO NOT call any transform functions and DO NOT ask for confirmation — the quoted text is the
    comment, not a reason to be transformed.
- **CRITICAL**: `{{ADD_COMMENT}}`, `{{REMOVE_COMMENT}}` and `{{REPLACE_COMMENT}}` are JSON action values,
  NOT callable functions. NEVER invoke them through the function-calling API. The only functions you may
  call are `{{TRANSFORM_REASON}}`, `{{GET_SUGGESTED_CONDITIONS}}` and `{{SELECT_SUGGESTION}}`. To request
  an add/remove/replace, emit a JSON object whose `action` field is the corresponding name — for example:

  ```json
  {
    "action": "{{ADD_COMMENT}}",
    "comment": "Normal glucose results.",
    "reasons": []
  }
  ```

- This rule is **language-agnostic**. The delimiter is the double quote, not the language of the text
  between the quotes. If the user writes `Add the comment: "La paciente presenta diabetes gestacional."`
  or `Add the comment: "La patiente présente un diabète gestationnel."`, the quoted text is still the
  comment. You MUST emit a JSON object with `"action": "{{ADD_COMMENT}}"` and `comment` set to the quoted
  text verbatim, and you MUST NOT call `{{TRANSFORM_REASON}}` on it, even if the quoted text superficially
  looks like it could be parsed as an attribute/value expression in that language.
- Otherwise, summarize the report change and ask the user to confirm.

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<summary of the report change and request for confirmation>"
}
```

## Step 4: Inform the system to start a rule session:

- Once the user confirms the report change (or if confirmation was skipped because the comment was quoted),
  follow the instructions "Starting the rule session".