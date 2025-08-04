# Generate Output

# Generate Output

Generate JSON objects that conform to the backend's ActionComment data class:

```kotlin
@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val comment: String? = null,
    val replacementComment: String? = null,
    val reasons: List<String>? = null
)
```

## For User-Facing Messages

When the output is a message to inform or prompt the user (not tied to an add, remove, or replace action), use the
following JSON structure:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your message to the user>"
}
```

- Use this structure for informational messages, confirmations, or prompts (e.g., asking the user to confirm an action).
- The message field must contain the exact message text, without unsolicited additions.
- Do not include comment, replacementComment, reasons, or debug fields unless explicitly required.

## For Actionable Requests

Once the action and reasons are confirmed, generate a JSON object based on the action type:

### For an Add Action

For an add action, use the following JSON structure:

```json
{
  "action": "{{ADD_ACTION}}",
  "comment": "<user entered comment text>",
  "reasons": [
    <array
    of
    reasons
    if
    provided,
    otherwise
    empty
    array>
  ]
}
```

- Set comment to the exact user-provided comment text.
- Do not include message, replacementComment, or debug unless explicitly required.

## For a Remove Action
```json
{
  "action": "{{REMOVE_ACTION}}",
  "comment": "<user entered comment text or identifier>",
  "reasons": [
    <array
    of
    reasons
    if
    provided,
    otherwise
    empty
    array>
  ]
}
```

- Set comment to the user-provided comment text or identifier.
- Do not include message, replacementComment, or debug unless explicitly required.

## For a Replace Action
```json
{
  "action": "{{REPLACE_ACTION}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered replacement comment text or identifier>",
  "reasons": [
    <array
    of
    reasons
    if
    provided,
    otherwise
    empty
    array>
  ]
}
```

- Set comment to the user-provided comment text or identifier for the comment to replace.
- Set replacementComment to the user-provided replacement comment text or identifier.
- Do not include message or debug unless explicitly required.

## JSON Formatting Rules

- Ensure the JSON is valid and properly formatted for the backend to process.
- Do not escape single quotes (') in any string fields; include them as-is.
- Escape double quotes (") and JSON-special characters (e.g., \, \n, \t) as required by JSON syntax.
- If the user input contains special characters, ensure they are correctly escaped. For example:

- User input: Let's "surf"

```json
{
  "action": "{{ADD_ACTION}}",
  "comment": "Let's \"surf\"",
  "reasons": []
}
```

Do not add unsolicited text (e.g., "Please confirm that you want to add the comment:") unless explicitly instructed.
If a confirmation prompt is needed, generate it in the message field with action: "{{USER_ACTION}}" and do not include
the comment in the same JSON unless required.

## Examples

1 User-Facing Message (e.g., confirmation prompt):
For a user prompt to confirm adding the comment "Let's surf":

```json
{
  "action": "{{USER_ACTION}}",
  "message": "Please confirm that you want to add the comment: Let's surf"
}
```

2. Add Action:
   For user input "Let's surf" with no reasons:

```json
{
  "action": "{{ADD_ACTION}}",
  "comment": "Let's surf",
  "reasons": []
}
```

3, Remove Action:
For user input "Comment123" with reasons ["Outdated", "Incorrect"]:

```json
{
  "action": "{{REMOVE_ACTION}}",
  "comment": "Comment123",
  "reasons": [
    "Outdated",
    "Incorrect"
  ]
}
```

4. Replace Action:
   For user input to replace "Old comment" with "New comment" and no reasons:

```json
{
  "action": "{{REPLACE_ACTION}}",
  "comment": "Old comment",
  "replacementComment": "New comment",
  "reasons": []
}
```

## Additional Notes

- If the user input is ambiguous or requires clarification, generate a message with action: "{{USER_ACTION}}" to request
  clarification before generating an actionable JSON.
- Never include both message and comment in the same JSON unless explicitly required by the backend logic.
- Ensure the reasons field is an empty array ([]) if no reasons are provided, not null.

