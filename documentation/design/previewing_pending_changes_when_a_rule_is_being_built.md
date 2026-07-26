# Previewing pending changes while a rule is being built

While the user is building a rule, the panels show what the rule *will* do
before it is committed. This document describes how that preview works for both
comments and derived attributes.

## 1. Behaviour

A rule session changes either the case's comments or one of its derived
attributes. Whichever it is, the relevant panel previews the change in place,
using a green background for something being added and red for something being
removed.

| Rule action             | Panel              | Preview                                                         |
|-------------------------|--------------------|-----------------------------------------------------------------|
| Add a comment           | Comments           | the new comment appended, **green**                             |
| Remove a comment        | Comments           | the existing comment, **red**                                   |
| Replace a comment       | Comments           | the old comment **red**, then the new one **green**             |
| Assign a derived value  | Derived attributes | a new name/value row, **green**                                 |
| Remove a derived value  | Derived attributes | the existing row, **red**                                       |
| Replace a derived value | Derived attributes | in the value cell, the old value **red** then the new **green** |

The preview disappears when the session is committed or cancelled.

Cornerstone cases are shown as they currently stand and are never given a
preview: the pending change belongs to the case under construction.

## 2. Why the preview cannot be derived from the case

During a session the rule tree has not been changed yet, so the case's
interpretation is still the pre-change one - the pending comment or value is not
present anywhere on the client. For a derived attribute the client also has no
way to compute it: only the server holds the `ValueExpression` and can evaluate
it against the session case.

The pending change therefore travels from server to client as its own value.

## 3. The model

`PendingChange` is the change a rule session is about to make. It has two
families of subtype, one per panel:

- **`Diff`** - a comment change: `Addition`, `Removal`, `Replacement`, each
  carrying comment text. This type is also used elsewhere for rule building and
  for diffing lists of sentences.
- **`DerivedValueChange`** - a derived attribute change, carrying an
  `attributeName`:
    - `DerivedValueAddition(attributeName, value, formula)`
    - `DerivedValueRemoval(attributeName)`
    - `DerivedValueReplacement(attributeName, newValue, newFormula)`

**Why `value` *and* `formula`.** They are shown in different places and neither
can be derived from the other on the client: the value cell shows the evaluated
result for this case, and the tooltip shows the formula text. They also vary
independently - the value is empty when the formula references an attribute the
case has no value for, while the formula is still worth showing, since it is
what explains why nothing will be assigned.

`CornerstoneStatus` carries **one** `pendingChange: PendingChange?`, plus two
read views for consumers that handle a single kind:

| Member             | Meaning                                              |
|--------------------|------------------------------------------------------|
| `pendingChange`    | the change, or null when no session is in progress   |
| `commentDiff`      | `pendingChange` as a `Diff`, else null               |
| `derivedValueDiff` | `pendingChange` as a `DerivedValueChange`, else null |

A session makes exactly one change, so this is one field rather than one per
panel. Separate nullable fields would allow a status to claim it was making two
changes at once - a state with no meaning that only tests could rule out.

`ruleConditions` on the same status carries the conditions added to the rule so
far, used by the tooltips.

## 4. Server

`RuleSessionManager` holds a single `currentChange: PendingChange?`. Each entry
point sets it as the session starts:

| Entry point                    | Sets                                                                                  |
|--------------------------------|---------------------------------------------------------------------------------------|
| add / remove / replace comment | the corresponding `Diff`, with comment text rendered for the case                     |
| assign a derived value         | `DerivedValueAddition`, value from evaluating the expression against the session case |
| remove a derived value         | `DerivedValueRemoval`                                                                 |
| replace a derived value        | `DerivedValueReplacement`, new value likewise evaluated                               |

Evaluation yields an empty value when a referenced attribute has no value in the
case. The change is still sent, so the user can see that the rule will assign
nothing to this case.

`currentChange` is set **before** the session starts, so the status returned by
the start call already carries it, and is rolled back if the session is refused

- otherwise a request rejected for, say, a dependency cycle would leave a stale
  preview for the next session to display. It is cleared on cancel and on commit.

`cornerstoneStatus()` passes `currentChange` and the rule conditions to the
client, whether or not there is a cornerstone to review.

## 5. UI

`CaseControl` is the single place that splits the change into the two panel
inputs, passing `commentDiff` to the comments side and `derivedValueDiff` to the
derived attributes side. Each panel therefore only ever sees a change of its own
kind, and no panel has to guess from content what it has been given.

### Comments

`InterpretationView` walks the real comments and, driven by the `Diff`, appends
the added text on `DIFF_ADDITION_COLOR` or restyles a matching existing comment
with `DIFF_REMOVAL_COLOR`.

### Derived attributes

`rowsToDisplay` is a pure function merging the case's derived values with the
pending change into the rows to draw, each carrying a highlight of `NONE`,
`ADDED`, `REMOVED` or `REPLACED`. Being free of Compose, it is unit tested
directly.

| Change      | Effect on the rows                                                                                                                                                      |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| none        | every row `NONE`                                                                                                                                                        |
| removal     | the matching row becomes `REMOVED`; no match means nothing is highlighted, which is correct - there is nothing to remove here                                           |
| replacement | the matching row becomes `REPLACED`, carrying the new value, with its formula swapped for the new one so the tooltip previews what the rule will install                |
| addition    | a synthetic row is inserted **in name order** and marked `ADDED`, taking the in-progress rule conditions as its tooltip conditions, since it has no rule of its own yet |

An addition naming an attribute that already has a value is **not** previewed;
the panel just shows the current state. This should not arise, because the user
is asked first (section 6); guessing that they meant a replacement would show
them a change they never asked for.

`DerivedValueRow` then renders each row:

- `ADDED` / `REMOVED` - the whole row is tinted, since the analogue of an added
  or removed comment is the whole name/value pair.
- `REPLACED` - the row background stays clear and only the value cell changes,
  showing the old value on red immediately followed by the new one on green.
  Keeping it to one row mirrors how the Comments panel renders a replacement and
  avoids duplicating the attribute name in the panel and the semantics tree.

## 6. Assigning to an attribute the case already has

If the user asks to assign a value to a derived attribute that already has one
for this case, they may have meant to replace it - but they may equally have
forgotten it was there, or be looking at the wrong case. So `AssignDerivedValue`
does not start a session; it names the current value and the expression
requested, and asks whether to replace it:

> "BMI" is already given for this case, with the value "30.93".
> Do you want to replace it with "weight / height ^ 3"?

The user can then answer with a plain yes, and the follow-up is routed to a
replacement. Nothing is previewed until they confirm.

This check precedes the more general "a derived attribute of that name already
exists, choose another" refusal, which remains for attributes that exist in the
KB but have no value on this case.

## 7. Testability

Background colour is invisible to both the Compose semantics tree and the
accessibility bridge the cucumber page objects use, so the highlight is also
exposed as a content description. The row keeps `DERIVED_VALUE_ROW_<name>` in
every state; the value cell carries the state:

| Highlight  | Value cell content description         |
|------------|----------------------------------------|
| `NONE`     | `DERIVED_VALUE_VALUE_<name>`           |
| `ADDED`    | `DERIVED_VALUE_PENDING_ADD_<name>`     |
| `REMOVED`  | `DERIVED_VALUE_PENDING_REMOVE_<name>`  |
| `REPLACED` | `DERIVED_VALUE_PENDING_REPLACE_<name>` |

Tests find the node and assert its rendered text is the expected value, or
`"<old> <new>"` for a replacement.

## 8. Where the code lives

| File                                              | Role                                           |
|---------------------------------------------------|------------------------------------------------|
| `common/.../model/diff/PendingChange.kt`          | the common supertype                           |
| `common/.../model/diff/Diff.kt`                   | comment changes                                |
| `common/.../model/diff/DerivedValueChange.kt`     | derived attribute changes                      |
| `common/.../model/rule/CornerstoneStatus.kt`      | carries `pendingChange` and the two read views |
| `server/.../kb/RuleSessionManager.kt`             | sets, clears and sends `currentChange`         |
| `server/.../kb/chat/action/AssignDerivedValue.kt` | asks before replacing an existing value        |
| `ui/.../casecontrol/CaseControl.kt`               | splits the change into the two panel inputs    |
| `ui/.../interpretation/InterpretationView.kt`     | renders the comment preview                    |
| `ui/.../interpretation/DerivedValueRows.kt`       | `rowsToDisplay` merge logic                    |
| `ui/.../interpretation/DerivedValuesPanel.kt`     | renders the derived attribute rows             |
