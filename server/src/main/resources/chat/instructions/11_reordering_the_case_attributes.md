# Reordering the case attributes

The interpreted case has a list of case attributes with a list of results for each attribute.

For the current case, the list of attributes is as follows:

{{ATTRIBUTES}}

The user may wish to move one of these attributes so that it comes before another.

If the user indicates that they would like the attributes re-ordered, output this result:

```json
{
  "action": "{{MOVE_ATTRIBUTE}}",
  "attributeMoved": "<user entered name of the attribute being moved>",
  "destination": "<user entered name of the attribute that will be immediately after the moved attribute once the move is done>"
}
```

