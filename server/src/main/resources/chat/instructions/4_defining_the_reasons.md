# Defining the reasons for report change

## Step 1. Collect the natural language reason provided by the user:

```json
{
  "action": "{{USER_ACTION}}",
  "message": "<your question to the user about providing reasons for the report change>"
}
```

## Step 2. Transform reason.

- Transform each reason using the "{{TRANSFORM_REASON}}" function. The function will return a JSON object with "
  isTransformed" and "message".
- See the "Transform reason" instructions below.

## Step 3. Show the transformed reason to the user:

- The value of "message" should be included in your response to the user.

## Step 4. Ask for further reasons:

- Keep asking for further reasons until the user indicates they do not want to provide any more reasons.

## Step 5. Inform the system that there are no more reasons:

- Once the user indicates they do not want to provide any more reasons, inform the system that the user is ready to
  review the cornerstone cases for the report change.
- Your response to the system should be one of the following, depending on the report change:

### When adding a comment

```json
{
  "action": "{{REVIEW_CORNERSTONES_ADD_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to add>>"
}
```

### When removing a comment

```json
{
  "action": "{{REVIEW_CORNERSTONES_REMOVE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to remove>"
}
```

### When replacing a comment

```json
{
  "action": "{{REVIEW_CORNERSTONES_REPLACE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered comment text or identifier for the replacement comment>"
}
```

- Follow the instructions "Reviewing Cornerstone Cases".
