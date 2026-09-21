# Showing conditions in the user's own words

## The idea

When a user gives the reason for a rule action (add, remove or replace a comment, assign a derived value), they type
a phrase in their own words, e.g. `elevated glucose`, and the system translates it to a formal condition,
`Glucose is high`. Previously only the formal text was shown again in tooltips and rule summaries.

The system remembers the user's phrase and shows it wherever the condition is displayed, with the formal text
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
status to the client. Conditions retain their existing case-insensitive ordering by formal text. Comment,
cornerstone and derived-value tooltips share the same renderer: a distinct non-blank phrase appears above the
formal text, which is smaller and grey. Otherwise only the formal text appears.

Formal-text accessibility identifiers remain unchanged. Phrase nodes use `CONDITION_PHRASE_PREFIX` followed by
the phrase, and each condition groups its phrase and formal text without merging their individual nodes.

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

`ConditionManager.getOrCreate` preserves this behaviour by returning the existing condition, including its phrase.

### Telling the user when the phrases differ

When a reason resolves to an existing condition whose stored phrase differs from what was typed, the "interpreted
as" message says so:

> Added your reason 'Glucose is high' (you previously called this 'elevated glucose').

`ConditionParsingResult.expression` carries the incoming wording separately from the stored condition. The note
appears when that wording is non-blank, differs from both formal text and stored phrase, and the stored phrase is
non-blank. Comparisons are exact. Otherwise the existing transformation response is retained. Edited suggestions
use the edited condition's formal text as the incoming expression, so they do not trigger the note.

The function handler preserves the message in its JSON response, and the chat instructions require the model to
relay it verbatim before the usual follow-up question. Validation failures retain their existing error message.

This is deterministic (the server compares the incoming phrase with the stored one), asks nothing, and gives the
user the information they need to decide whether to rename. It fits the chat guidelines: no question, no control,
an ordinary follow-up message renames if wanted.

### Renaming the phrase

The `RenameCondition(conditionText, newPhrase)` chat action identifies a stored condition by its formal text or
current phrase, ignoring case and surrounding whitespace. A blank new phrase, a missing match or multiple matches
are refused. Ambiguous matches list the formal conditions. The new phrase is preserved as supplied; the formal
predicate and condition id remain unchanged.

Conditions remain immutable. `ConditionManager.renamePhrase` persists a copy before replacing its cache entry.
`RuleTree.replaceCondition` replaces references by id in each rule, and `RuleSessionManager` also refreshes the
active session's conditions. This keeps current summaries, comments, derived values and cornerstone tooltips in
sync without rebuilding rules or changing inference. Reloading the KB reconstructs rules from the updated store.

The action can run during rule building and pushes cornerstone status when a session is active. The ordinary case
refresh after chat reinterprets the current case. In-memory updates replace the entry with the same id; PostgreSQL
updates the existing condition JSON without a schema change. The PostgreSQL tests are supplied for the user to run.

### Attribute renames

The phrase is free text, so renaming `Glucose` to `Glucosa` leaves `elevated glucose` unchanged while `asText()`
updates. Showing both texts is the mitigation; no attempt is made to rewrite phrases.
