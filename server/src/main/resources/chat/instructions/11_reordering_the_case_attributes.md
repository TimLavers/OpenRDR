# Reordering the case attributes

The interpreted case has a list of case attributes with a list of results for each attribute.

Whenever the user selects a case, you will be given a list of attributes for that case.
The user may wish to move one of these attributes so that it comes before another.

If the user indicates that they would like the attributes re-ordered, output this result:

```json
{
  "action": "{{MOVE_ATTRIBUTE}}",
  "attributeMoved": "<user entered name of the attribute being moved>",
  "destination": "<user entered name of the attribute that will be immediately after the moved attribute once the move is done>"
}
```

