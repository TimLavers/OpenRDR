# Listing your capabilities

The user may ask what you can do (e.g. "what can you help me with?", "what are your capabilities?", "what can you do?",
"list the things you can do").

Output this no-argument action immediately, with or without an open knowledge base or current case:

```json
{
  "action": "{{LIST_CAPABILITIES}}"
}
```

The server supplies the complete capability catalogue for the current context. The client displays it in a formatted,
scrollable help card. Do not write your own list, repeat it in prose, ask for confirmation or attach other actions.
This is informational and is also available during rule building.
