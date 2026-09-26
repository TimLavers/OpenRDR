# Previewing pending changes during a rule session

While a rule is being built, the Comments or Derived attributes panel shows what the rule *will* do to the current case,
in place: green for something added, red for something removed, and for a replacement the old in red then the new in
green. The preview disappears on commit or cancel. Cornerstones are shown as they stand and never previewed: the pending
change belongs to the case under construction.

| Rule action             | Panel              | Preview                                                                    |
|-------------------------|--------------------|----------------------------------------------------------------------------|
| Add a comment           | Comments           | a new name/comment row appended, green                                     |
| Remove a comment        | Comments           | the row, red                                                               |
| Replace a comment       | Comments           | one row: the old name and comment red, then the new name and comment green |
| Assign a derived value  | Derived attributes | a new name/value row in name order, green                                  |
| Remove a derived value  | Derived attributes | the row, red                                                               |
| Replace a derived value | Derived attributes | in the value cell, old red then new green                                  |

A comment replacement shows two names because it is a change of *attribute*; a derived-value replacement changes one
attribute's value, so the name is not repeated.

## Why the change travels as its own value

During a session the rule tree is unchanged, so the case's interpretation is still the old one: the pending comment or
value exists nowhere on the client. For a derived value the client could not compute it anyway, since only the server
holds the expression. So `RuleSessionManager` holds one `PendingChange` for the session and sends it in every
`CornerstoneStatus`, along with the rule's conditions so far for the tooltips.

`PendingChange` has two families, `Diff` for comments and `DerivedValueChange` for derived values, each carrying the
attribute *name* so that the panel can show it before the rule exists. A derived-value change carries both the
evaluated value and the formula text: they are shown in different places, neither can be derived from the other on the
client, and the value is empty (while the formula is still worth showing) when an input is missing from the case.

The status carries **one** nullable `pendingChange`, not one per panel: a session makes exactly one change, and two
fields would allow a status to claim two changes at once, a state with no meaning.

The name is read from the attribute when the status is built, not snapshotted at session start, because the user can
rename a comment during the session; `RenameAttribute` pushes a fresh status so the panel follows.

The change is set before the session is started and rolled back if the start is refused, so that a request rejected for,
say, a cycle cannot leave a stale preview for the next session.

## Assigning to an attribute the case already has

If the user asks to assign a derived value the case already has, they may mean replace it, or may have forgotten it is
there, or be on the wrong case. `AssignDerivedValue` therefore starts nothing and asks, naming the current value and the
requested expression; a plain yes is routed to a replacement. Nothing is previewed until they confirm, because
guessing would show them a change they did not ask for.

## Client

`CaseControl` splits the change into the two panels' inputs, so each panel only ever sees a change of its own kind. Each
panel has a pure, Compose-free merge function (`commentRowsToDisplay`, `rowsToDisplay`) that combines the case's rows
with the pending change into rows carrying a highlight state, unit-tested directly. An added row takes the in-progress
rule's conditions as its tooltip, having no rule of its own yet; a removal is matched by attribute name so that a
comment
with a variable, whose text differs case to case, still matches.

Background colour is invisible to the accessibility tree the acceptance tests read, so the highlight state is also
exposed as the cell's content description (`…_PENDING_ADD_<name>` and so on), and every id takes a prefix so that the
case's and the cornerstone's tables can be on screen together.
