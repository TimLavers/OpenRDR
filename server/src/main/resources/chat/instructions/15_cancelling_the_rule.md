# Cancelling the rule

If the user indicates that they want to cancel the current rule session (e.g. "cancel this rule", "cancel", "stop
building this rule"), output the following:

```json
{
  "action": "{{CANCEL_RULE}}"
}
```

This will cancel the rule session without committing any changes. Do not confuse this with undoing a previously
committed rule — cancelling applies to a rule session that is still in progress.
