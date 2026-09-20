# Showing conditions in the user's own words

Status: stage 1 implemented. Both texts reach the client; phrase display and renaming are still pending.

## The idea

When a user gives the reason for a rule action (add, remove or replace a comment, assign a derived value), they type
a phrase in their own words, e.g. `elevated glucose`, and the system translates it to a formal condition,
`Glucose is high`. Today only the formal text is ever shown again: in the comment tooltip, in the cornerstone view
and in the rule summary.

The proposal is to remember the user's phrase and show it wherever the condition is displayed, with the formal text
alongside it as the authoritative form.

## Rationale

- **Familiarity.** The expert re-reads their rules far more often than they write them. A condition shown in the
  words they chose is recognised at a glance; the formal text has to be re-parsed each time.
- **Domain vocabulary.** Experts think in their own terminology (`hyperglycaemia`, `raised TSH`), which is often
  more specific than the formal predicate vocabulary (`high`, `low`, `normal`).
- **Language.** The phrase is opaque text, so a Spanish-speaking user sees `glucosa elevada` without the system
  knowing anything about Spanish. The only language-aware step is the one that already exists, the LLM translation in
  `ConditionGenerator`.
- **Auditability.** Keeping the user's phrase next to the formal text lets a later reader check that the translation
  was what the author meant, which is the one thing the formal text alone cannot show.

## What already exists

Most of the storage side is already built:

- Every condition class (`EpisodicCondition`, `SeriesCondition`, `CaseStructureCondition`) has a `userExpression`
  field, serialised and persisted with the condition.
- `ConditionGenerator.conditionFor` fills it with the phrase the user typed.
- `Condition.sameAs` ignores it, and `ConditionManager.getOrCreate` dedupes on `sameAs`, so a phrase is attached to a
  condition only when that condition is first created.
- Its only consumer is `ReasonTransformation`, which compares it with `asText()` to decide whether to tell the user
  "I interpreted that as ...".

Conditions created from suggestions, or by editing a suggestion's value, have a blank `userExpression` or one equal
to `asText()`.

What is missing is display, and a way to change the phrase.

## Design

### The phrase belongs to the condition, not the rule

A condition is stored once and referenced by id from every rule that uses it. The phrase is an alias for that
condition, so it lives on the condition. Storing it per rule would let one condition read differently in two rules,
which defeats the familiarity argument.

### Display

Wherever a condition is shown (comment tooltip, cornerstone tooltip, rule summary), show the phrase as the primary
text and the formal text as the secondary text, e.g. `elevated glucose` with `Glucose is high` beneath or on hover.
Never show the phrase alone: the formal text is the ground truth, and the phrase may be stale after an attribute
rename or may have been mistranslated.

When `userExpression` is blank or equal to `asText()`, show `asText()` only. This is the case for suggested and
edited conditions.

`ConditionText(formal, phrase)` now carries both texts through `Rule.conditionsFromRoot()`,
`RuleSummary.conditionsFromRoot`, interpretation assignments, rendered comments, derived values and cornerstone
status to the client. Conditions retain their existing case-insensitive ordering by formal text. In stage 1,
tooltips and their accessibility descriptions still render formal text only; phrase display is the next stage.

### Text supplied to the language model

Condition lists supplied to chat and system prompts continue to use `Condition.asText()`, including
`RuleSessionManager.currentRuleSessionConditionTexts()`. Stored phrases are display metadata; the model translates
the user's wording to formal conditions and should not receive those aliases back as rule predicates.

### Two phrases for one condition: first phrase wins

Scenario: the user builds one rule with `elevated glucose` and later another with `raised glucose`. Both translate
to `Glucose is high`, which is a single stored condition. Which phrase is shown?

The first one. Reasons:

- **Shared condition.** Every rule using the condition displays the same phrase. Last-write-wins would silently
  change the appearance of rules the user has already reviewed, and make the KB's appearance depend on the order in
  which rules were built.
- **Familiarity is the point.** A stable association between one phrase and one formal condition is what makes the
  phrase useful. Replacing it on every use erodes the association.
- **The change is available on request.** Changing the phrase is a one-field update on the stored condition and has
  a natural chat form, mirroring the existing rename actions for attributes and comments.
- **Do not accumulate synonyms.** A list of phrases per condition has nothing sensible to display. One phrase per
  condition.

This is also what the code does now, by accident of `getOrCreate` returning the existing condition.

### Telling the user when the phrases differ

When a reason resolves to an existing condition whose stored phrase differs from what was typed, the "interpreted
as" message says so:

> `raised glucose` → `Glucose is high` (you previously called this `elevated glucose`).

This is deterministic (the server compares the incoming phrase with the stored one), asks nothing, and gives the
user the information they need to decide whether to rename. It fits the chat guidelines: no question, no control,
an ordinary follow-up message renames if wanted.

### Renaming the phrase

A chat action, `RenameCondition` or similar, taking the condition (by formal text or current phrase) and the new
phrase. Refuse a blank phrase. The formal text is not editable this way; changing the condition itself is a different
operation.

### Attribute renames

The phrase is free text, so renaming `Glucose` to `Glucosa` leaves `elevated glucose` unchanged while `asText()`
updates. Showing both texts is the mitigation; no attempt is made to rewrite phrases.

## Implementation steps

1. Display: carry both texts to the client and render phrase-with-formal-text in the comment tooltip, cornerstone
   tooltip and rule summary, falling back to `asText()` when the phrase is blank or identical.
2. The "you previously called this" note in `ReasonTransformation`.
3. The rename action and its chat instruction.

Each step is independently useful and can land on its own.
