# Giving the rule action and its reasons in one instruction

## Motivation

A user working from a script knows both the report change and its reasons before they start typing. The script the
oncology demonstration follows reads, per case:

    Please add derived attribute "activating" with value "true"
    reasons "driverRole is ONCOGENIC" and "driverInterp is HIGH".

    add comment {gene} ({protein impact})
    reason "activating is true"

    replace comment "activating mutation." with comment "activating mutation, possible indication for ALK inhibitors."
    with reason "gene is ALK"

Today each of these costs two or three chat turns: the action, then each reason typed on its own. We want the one
instruction to start the session, add the conditions, show the (filtered) suggestions and the first surviving
cornerstone, and ask "Do you want to provide any more reasons?" — landing where the conversation would be after the
reasons had been typed one by one.

## Current behaviour

- `3_defining_the_report_change.md` already shows `"reasons": []` in its `ADD_COMMENT` example, but `ActionComment` has
  no `reasons` field, so whatever the model puts there is discarded.
- `5_starting_the_rule_session.md` forbids calling `transformReason` before the session has started and says the comment
  text is not a reason, so a reason given up front is usually lost; sometimes the model passes it to `transformReason`
  in its follow-up turn, sometimes it asks for a reason it has already been given.

## Design

### The model transcribes; the server decides

The model's only new job is to split the instruction into the action and a list of reasons, and put the reasons,
**verbatim**, in a `reasons` array on the action JSON. It does not call `transformReason` for them. This is the same
division of labour as for comment text, value expressions and suggestion numbers: the model never rewrites a condition.

```json
{
  "action": "AssignDerivedValue",
  "attributeName": "activating",
  "valueExpression": "true",
  "reasons": [
    "driverRole is ONCOGENIC",
    "driverInterp is HIGH"
  ]
}
```

The array is accepted on all six session-starting actions: `AddComment`, `RemoveComment`, `ReplaceComment`,
`AssignDerivedValue`, `RemoveDerivedValue`, `ReplaceDerivedValue`. It is optional and defaults to empty, so every
existing conversation is unchanged.

Splitting rules for the model, given in the instructions:

- Reasons follow a marker: `reason`, `reasons`, `with reason(s)`, `because`, `since`. Everything after the marker is
  reasons; a quoted string is one reason; several are separated by `and`, `;` or a new line. Quotes are stripped, the
  text inside is copied as written.
- The comment or value expression is everything before the marker, per the existing rules (quoted → no confirmation;
  `{name}` placeholders are variables, not reasons).
- No marker → no reasons. The message is handled exactly as today.
- When confirmation is needed (unquoted comment), the confirmation names the reasons too, and the confirmed action
  carries them.

### The server applies the reasons as the session starts

Immediately after `startRuleSessionTo…` succeeds, and before the model is told anything, the server applies the reasons
in order through the path a typed reason takes: `conditionForExpression`, then `addConditionToCurrentRuleSession`.
Each reason succeeds or fails on its own; a failure does not undo the session or the other reasons, which is what
happens today when reasons are typed one at a time. The cornerstone status is pushed once, after the last one.

If the action does not start a session (rule session already active, name clash, "did you mean" question, refused
expression) the reasons are simply not applied. The user re-sends the instruction after answering, as they would today.

### What the model is told

The summary the action sends to the model (currently just the cornerstone line) gains the outcome:

    Cornerstone: Bondi, Index: 0, Total: 1
    The system has already applied the user's reasons:
      added: driverRole is "ONCOGENIC"
      not understood: "driverInterp HIGH" — <server's message>
    Do NOT ask for a first reason and do NOT call transformReason for these. Call getSuggestedConditions now.

So the model's history is correct (it can still answer "list the reasons I have added"), and it calls
`getSuggestedConditions` as it does now, which is what fills the suggestion chips.

### What the user sees

The model's follow-up message is replaced by the server's, exactly as `RuleConversation.completeTurn` does after a typed
reason:

    Comment named C3. …                                  (existing line, comment adds only)
    Added your reason 'driverRole is "ONCOGENIC"'.       (or the failure message, per reason, in order)
    Do you want to provide any more reasons?

with the suggestions attached (already filtered of conditions in the rule by `ChatResponseEnricher`), the cornerstone
panel showing the first surviving cornerstone, and `RuleConversation` in `AwaitingReasonReply`. From here the flow is
the
existing one: more reasons, decline, allow/disallow cornerstones, commit.

### Why the reply is composed where the reasons are applied, not by `completeTurn`

The session-starting actions call `modelResponder.response(summary)`, which is a nested `ChatManager.response` turn.
That turn clears `ReasonAcknowledgements` and snapshots `conditionsBefore` *after* the reasons were added, so
`completeTurn` sees nothing new and would let the model's message stand. The acknowledgement is therefore built by the
code that applies the reasons and returned by the action; `ChatManager` then puts `RuleConversation` into
`AwaitingReasonReply`. The wording is shared with `completeTurn`, so there is one way of saying "a reason was added".

## Non-goals

- No parsing of `because` on the server. The split is the model's job; the server sees a list.
- No one-shot commit (`… and commit`). Cornerstone review stays a conversation.
- No reasons on non-session actions (rename, reorder, edit definition, KB management).
- No new GUI control. This is text in the chat, per `chat_ui_guidelines.md`.
- Selecting the case is a separate step, as in the script.

## Risks

- **Mis-splitting.** A model that puts a reason into the comment text, or vice versa, produces the wrong rule. The
  marker is explicit and the reasons are quoted in the script, which is the easiest case. The cukes pin both the
  single- and multi-reason forms; if the model proves unreliable the fallback is a deterministic server-side split on
  the marker words, which the design leaves room for since the server owns the list.
- **A reason that cannot be parsed** gets the message it would get if typed alone, and the user is asked for more, so
  nothing is dropped silently.
- **Duplicate condition** (a reason that is also a suggestion the user then clicks) is already handled: the enricher
  removes conditions in the rule from the suggestions.
