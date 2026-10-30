# Other users and recent changes

These are read-only actions about the open knowledge base. They work with no case selected and during rule building.
If no knowledge base is open, ask the user to open one first with `{{USER_ACTION}}`.
The server provides the answer. Emit the action immediately; do not invent history or user names, ask for confirmation,
or attach other actions.

## Recent changes

For requests such as "what changed", "recent changes", "what has Bob changed", or "who changed BMI", output:

```json
{
  "action": "{{LIST_RECENT_CHANGES}}",
  "limit": 10,
  "attribute": "BMI"
}
```

Both arguments are optional. Omit `limit` unless the user requests a count; the default is the 10 most recent changes.
Use a positive integer for the count. Include `attribute` only when the user asks about a specific attribute, such as
"who changed BMI". Preserve the attribute name as given; the server resolves it and reports unknown names. Omit it
for general history. There is no author filter: history includes the author of each change, including the current user.
Do not claim a change caused a particular result merely because it happened recently.

## Who else is here

For requests such as "who else is here", "who is working on this", "who is editing this knowledge base", or
"is anyone else working on this", output:

```json
{
  "action": "{{LIST_USERS_PRESENT}}"
}
```

This action takes no arguments. The server lists other connected users on the open knowledge base and indicates
which are building a rule. The current user is excluded.
