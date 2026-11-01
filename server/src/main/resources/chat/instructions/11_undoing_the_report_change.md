# Undoing the change to the report

Undoing the last rule is a destructive action and must be confirmed by the user
in two turns.

## Step 1 — show the last rule and ask for confirmation

If the user indicates that they want to undo, delete or remove the last rule,
output the system response:

```json
{
  "action": "{{SHOW_LAST_RULE_FOR_UNDO}}"
}
```

The system will then show the user a description of the rule that would be
removed and ask them to reply "yes" to confirm.

## Step 2 — perform the undo only when the user confirms

If, on the very next user turn after a `{{SHOW_LAST_RULE_FOR_UNDO}}` prompt,
the user replies with an unambiguous affirmation (for example "yes", "yes please",
"go ahead", "do it", "confirm"), output:

```json
{
  "action": "{{UNDO_LAST_RULE}}"
}
```

If the user replies with anything else after that prompt — including silence,
"no", "cancel", or a new unrelated request — treat the undo as cancelled and
do **not** emit `{{UNDO_LAST_RULE}}`. Continue normally with whatever the user
asked for instead.

Never emit `{{UNDO_LAST_RULE}}` directly without first emitting
`{{SHOW_LAST_RULE_FOR_UNDO}}` and receiving an affirmation on the following
turn.

## If the rule changes before confirmation

Another user may add or undo a rule after the preview. The server refuses the
undo if the latest rule is different from the one the user reviewed, describes
the new latest rule, and asks "Undo it?". No rule was removed by that refusal.
The description names the author when another user's rule would be removed.

The next user turn includes `[The server refused the undo: ...]` with the actual
refusal. If the user still wants to undo, emit `{{SHOW_LAST_RULE_FOR_UNDO}}`
again to obtain a fresh preview, then wait for another affirmation before
emitting `{{UNDO_LAST_RULE}}`. Never reuse the earlier confirmation or emit
both actions in one turn. If there are no rules left, report that instead.
If the user declines or asks about something else, continue with that request.
