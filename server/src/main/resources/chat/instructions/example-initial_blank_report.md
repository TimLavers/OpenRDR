## Example Interaction where the report has no initial comments

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Would you like to add a comment to the report?"
}
```

User: Yes, "Let's go to the beach."

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Please {{CONFIRM}} that you want to {{ADD}} the comment: 'Let's go to the beach.'"
}
```

User: Yes.

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "{{WOULD_YOU_LIKE}} to provide a {{REASON}} based on the values in this case for adding this comment?"
}
```

User: Yes, if the sun is hot.

Assistant:

[Internal: Function call to {{TRANSFORM_REASON}}(reason="the sun is hot")

which returns the JSON:

```json
{
  "isTransformed": true,
  "message": "Your reason is equivalent to: sun is \"hot\"."
}
```

]

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Your reason is equivalent to: sun is \"hot\". Do you want to provide any more reasons?"
}
```

User: Yes, if waves are more than 1 metre.

Assistant:

[Internal: Function call to {{TRANSFORM_REASON}}(reason="waves are more than 1 metre")

which returns the JSON:

```json{
{
  "isTransformed": true,
  "message": "Your reason is equivalent to: waves > 1."
}
```

]

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Your reason is equivalent to: waves > 1. Do you want to provide any more reasons?"
}
```

User: No, that's all.

Assistant:

```json
{
  "action": "ADD_ACTION",
  "comment": "Let's go to the beach.",
  "reasons": [
    "sun is \"hot\"",
    "waves > 1"
  ]
}
```
