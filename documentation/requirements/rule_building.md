# Rule building

## The RDR workflow

An expert reviewing a case's interpretation decides that something is wrong and says what should change: the **rule
action**. They then say **why**, as one or more conditions true for this case: the **reasons**. The system shows the
**cornerstone cases** whose interpretation the new rule would also change; for each, the expert either allows the change
or adds a condition that excludes the cornerstone. Finally the rule is **committed**. A rule is never edited; a mistake
is corrected by a refinement rule or by undoing the last rule.

The whole workflow is conducted in the chat. There are no rule-building controls in the GUI; the case, cornerstone and
pending-change panels show state, the chat carries intent.
See [design/chat_architecture.md](../design/chat_architecture.md).

## Rule actions

| Action                  | Validation                                           |
|-------------------------|------------------------------------------------------|
| Add a comment           | `rulebuilding/Build rule to add comment.feature`     |
| Remove a comment        | `rulebuilding/Build rule to remove comment.feature`  |
| Replace a comment       | `rulebuilding/Build rule to replace comment.feature` |
| Assign a derived value  | `inferencing/Derived attribute.feature`              |
| Remove a derived value  | `inferencing/Derived attribute.feature`              |
| Replace a derived value | `inferencing/Derived attribute.feature`              |

The action is stated by the user, not inferred from an edited report: a trial of deriving actions from a free-text edit
with a sentence-level diff was abandoned because the diff was often wrong and the user could not see why. One action
per rule is the RDR discipline, and stating it is one sentence.

| Requirement           | Description                                                                                                                                                   | Validation                                       |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| Confirm unquoted text | A comment given without quotes is read back for confirmation before the session starts, since the model may have trimmed it. Quoted text is taken as written. | `chat/Add comments without conditions.feature`   |
| Existing comment      | Adding a comment whose text is already in the knowledge base reuses that comment.                                                                             | `rulebuilding/Build rule to add comment.feature` |
| Preview               | From the moment the session starts, the Comments or Derived attributes panel shows the pending change in place, green for added and red for removed.          | `rulebuilding/*.feature`                         |

## Reasons

| Requirement     | Description                                                                                                                                                                                                                                                       | Validation                                                                                                   |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Own words       | A reason is typed (or spoken) in the user's own words and language; the system translates it to a condition and acknowledges it in the formal form, e.g. `Added your reason 'Glucose is high'`.                                                                   | `rulebuilding/Conditions in the users words.feature`, `chat/Build rules using non-English languages.feature` |
| Must hold       | A reason that does not hold for the case, names an attribute the case lacks, or cannot be understood is refused with the cause, and nothing is added.                                                                                                             | `chat/Add comments with conditions.feature`                                                                  |
| With the action | The action and its reasons may be given in one instruction (`Add the comment "X" reason "Y" and "Z"`). The reasons are applied as the session starts, each succeeding or failing on its own, and the conversation continues as if they had been typed one by one. | `chat/Rule action with reasons.feature`                                                                      |
| Suggestions     | After each reason the chat offers a ranked list of conditions that hold for the case, as clickable chips; a chip is a shortcut for typing the condition. Conditions already in the rule are not offered. Editable suggestions (`TSH ≥ __`) can be adjusted.       | `conditions/Suggested Conditions.feature`, `conditions/Targeted Suggested Conditions Phase 1.feature`        |
| Ask for more    | After every reason the chat asks whether there are more, so that the expert, not the model, decides when the justification is complete.                                                                                                                           | `chat/Add comments with conditions.feature`                                                                  |
| List and remove | The user can ask which reasons the rule has so far and remove one by naming it; the cornerstones are re-evaluated.                                                                                                                                                | `chat/Manage rule conditions.feature`                                                                        |
| Previous phrase | When a reason resolves to a condition the user once described differently, the acknowledgement says so (`you previously called this 'elevated glucose'`).                                                                                                         | `rulebuilding/Conditions in the users words.feature`                                                         |

## Cornerstones

| Requirement           | Description                                                                                                                                                                       | Validation                                                             |
|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| Which cases           | A cornerstone is shown only if the rule would change its interpretation, judged after inference has run to a fixpoint, so indirect effects through derived values count.          | `rulebuilding/Cornerstone cases.feature`                               |
| Shown beside the case | The cornerstone under review is shown in a panel beside the current case, with its own comments and derived values, so the expert compares data rather than reading a transcript. | `kb/Cornerstone review.feature`                                        |
| Allow                 | The user may allow the change for the cornerstone (`allow`), and it is set aside.                                                                                                 | `chat/Navigate cornerstones.feature`                                   |
| Exclude               | Adding a reason that is false for the cornerstone removes it from the list; the next surviving cornerstone is shown.                                                              | `rulebuilding/Cornerstone cases.feature`                               |
| Navigate              | The user can move to the next or previous cornerstone and ask how many remain.                                                                                                    | `chat/Navigate cornerstones.feature`, `chat/Show cornerstones.feature` |
| Count                 | The chat states the number of cornerstones remaining after every change.                                                                                                          | `chat/Show cornerstones.feature`                                       |

## Finishing

| Requirement  | Description                                                                                                                                                        | Validation                                                            |
|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| Commit       | When the user has no more reasons and every cornerstone is allowed or excluded, the rule is committed and the case re-interpreted. The case becomes a cornerstone. | `rulebuilding/Build rule.feature`                                     |
| Cancel       | The user may cancel the session at any point; nothing changes.                                                                                                     | `chat/Manage rule conditions.feature`                                 |
| Undo         | The last committed rule can be undone; the chat first shows what it was.                                                                                           | `rulebuilding/Undo rule.feature`, `chat/Remove previous rule.feature` |
| Exclusive    | While a rule is being built, requests that change what the chat is about (open, create, delete, import or export a knowledge base) are refused.                    | `kb/Knowledge Base Management.feature`                                |
| Show reasons | Hovering a comment or derived value shows the conditions, from the root rule down, that gave it.                                                                   | `rulebuilding/Show conditions for rule action.feature`                |

## Building rules without the GUI

Rules can also be built through REST endpoints, used by the sample knowledge-base builders and the acceptance tests
(`rulebuilding/Build rule from parsed expression.feature`, `samples/*.feature`).
