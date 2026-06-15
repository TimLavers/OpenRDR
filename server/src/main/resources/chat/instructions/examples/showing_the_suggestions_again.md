## Example interactions where the user asks to see the suggestions again

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Please show the suggestions again"
}
```

User: Add "Let's bring flippers."

Assistant (the user provided the comment in quotes (the apostrophe in "Let's" is part of the comment, not a quote
delimiter), so skip confirmation):

```json
{
  "action": "UserAction",
  "message": "Here are the suggestions again:"
}
```

System:

```json
{
  "cornerstoneToReview": null,
  "indexOfCornerstoneToReview": -1,
  "numberOfCornerstones": 0
}
```

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "{{WOULD_YOU_LIKE}} to provide a {{REASON}} for adding this comment?"
}
```

User: Yes

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "What is the {{FIRST_REASON}}?"
}
```

User: Big waves

Assistant:

[Internal: Function call to {{TRANSFORM_REASON}}(reason="waves are high")

which returns the JSON:

```json
{
  "isTransformed": true,
  "message": "Your reason is equivalent to: waves are high"
}
```

]

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Your reason is equivalent to: waves are high. Do you want to provide any more reasons?"
}
```

User: No, that's all.

Assistant:

```json
{
  "action": "{{COMMIT_RULE}}"
}
```
