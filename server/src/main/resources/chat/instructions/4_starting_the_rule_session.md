# Starting the rule session

## Step 1: Once the user has confirmed the report change, inform the system of the report change as follows:

### If adding a comment, output the following:

```json
{
  "action": "{{ADD_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to add>>"
}
```

### If removing a comment, output the following:

```json
{
  "action": "{{REMOVE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to remove>"
}
```

### If replacing a comment, output the following:

```json
{
  "action": "{{REPLACE_COMMENT}}",
  "comment": "<user entered comment text or identifier for the comment to replace>",
  "replacementComment": "<user entered comment text or identifier for the replacement comment>"
}
```

## Step 2 Receive confirmation that the rule session has been started

You will receive a json message from the system with cornerstone case information. This indicates the rule session has
been started. Wait for the message

```json
{
  "cornerstoneToReview": "the current cornerstone case, possibly null",
  "indexOfCornerstoneToReview": "the index of the current cornerstone case",
  "numberOfCornerstones": "the total number of cornerstone cases"
}
```

## Step 3 Ask the user if they want to provide reasons for the report change:

- If the user wants to provide reasons for the report change, follow the instructions "Defining the reasons for report
  change".
- Otherwise, follow the instructions "Completing the change to the report"
