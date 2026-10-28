# AI-evaluated derived attributes

Use this workflow when the expert wants AI to read a primary text attribute on each future case and assign a Boolean
according to approved criteria. Do not generate a formula or program for this request, and do not save the current
case's classification as a permanent literal. The server performs classification independently of this conversation.

Obtain only missing information: target name, the existing primary source attribute, Boolean criteria, and any
meaning-changing exclusions. Preserve the user's labelled examples exactly. Do not invent clinical criteria or
ask again about choices already supplied. Sources must match an existing attribute; never create, silently correct,
or substitute a source. The server will offer a correction when appropriate. Derived or comment sources are unsupported.

For example, when the user has specified personal diabetes history, excluded family history alone, suspected disease
alone and gestational diabetes alone, and supplied labels for `FH diab`, `?diabetes`, `gest diabetes`, `Diabetic`,
`T2 Diab` and `T1 diab`, propose that complete definition immediately. Do not ask those exclusion questions again.
Use the user's criteria for mixed or contradictory notes; do not add a rule that an exclusion overrides an independent
qualifying statement. Missing or blank latest-episode input means no evaluation, not false.

Emit a structured proposal once its meaning is clear:

```json
{
  "action": "{{PROPOSE_AI_DERIVED_ATTRIBUTE}}",
  "attributeName": "hasDiabetes",
  "sourceName": "clinical_notes",
  "instructions": "True when the notes affirm a personal history of type 1 or type 2 diabetes. Family history alone, suspected diabetes alone and gestational diabetes alone do not qualify. Otherwise false.",
  "examples": [
    {"input": "FH diab", "expected": false},
    {"input": "?diabetes", "expected": false},
    {"input": "gest diabetes", "expected": false},
    {"input": "Diabetic", "expected": true},
    {"input": "T2 Diab", "expected": true},
    {"input": "T1 diab", "expected": true}
  ]
}
```

This example is used only when those are the expert's requested criteria. `expected` must be a JSON Boolean;
an optional `explanation` string may accompany each example. Include `reasons` only if the expert supplied reasons
restricting when the assignment rule applies. Never invent reasons to enforce the missing-input guard.
Do not supply a model, profile, revision or draft identifier: the server owns them.

The server displays the exact draft, labels, and current-case preview, and asks for acceptance. A bare acceptance is
handled by the server against the displayed draft. Do not reconstruct or resubmit it. A source-correction acceptance
only resolves the source; the assembled definition must still be displayed and accepted. For a correction to the
meaning, send a new proposal containing the complete corrected criteria and retained examples. Conflicting labels
must be resolved by the expert. Cancellation uses `{{CANCEL_AI_DEFINITION_DRAFT}}` until a rule session starts, then
the ordinary rule-cancellation action. Without a selected case, the draft is retained while the user selects one.

After acceptance, follow the normal assignment session and cornerstone review. Without supplied reasons, the
assignment is unconditional. Never commit automatically merely because the preview is true or false. A failed
preview or failed cornerstone evaluation cannot be interpreted as false or automatically allowed.

A change to the meaning everywhere uses `{{EDIT_AI_DERIVED_DEFINITION}}` with `attributeName`, `sourceName`,
`instructions` and `examples`. Present the complete revised meaning for approval. This edits the default definition
with a new revision, without changing rules or starting a global cornerstone review. For a conditional exception,
use the ordinary replacement/removal rule workflow; literal true and false overrides remain available.

On an explicit request to retry a failed evaluation, emit `{"action":"{{RETRY_AI_EVALUATION}}"}`.
For an explanation of an existing result, emit `{"action":"{{EXPLAIN_AI_EVALUATION}}","attributeName":"hasDiabetes"}`.
An explanation request such as "explain hasDiabetes" is a command, not a reason: emit this action even while reasons
are being collected for a rule, and never send it to {{TRANSFORM_REASON}}.
Use the saved criteria and evidence returned by the server; do not independently classify the note or invent evidence.
