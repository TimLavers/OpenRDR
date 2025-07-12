## Example Interaction where the report has an initial comment:

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "This case has the following comment:\n\"Go to the beach.\".\n{{WOULD_YOU_LIKE}} to add another one, or replace or remove this one?"
}
```

User: Add "Bring flippers."

Assistant:

```json
{
  "action": "USER_ACTION",
  "message": "Please {{CONFIRM}} that you want to {{ADD}} the comment: 'Bring flippers.'"
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
  "action": "ADD_ACTION",
  "comment": "Bring flippers.",
  "reasons": [
    "waves are high"
  ]
}
```
