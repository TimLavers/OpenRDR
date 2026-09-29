# Comments

Requirements: [comments.md](../requirements/comments.md). The modelling of a comment as an attribute is in
[rule_tree_and_inference.md](rule_tree_and_inference.md); this doc covers what is specific to comments as text.

## Variables

A comment's definition is a `CommentTemplate(text, variables)`: the text with each variable replaced by the positional
token `${}`, and one `Attribute` per token in order of appearance. The user-facing form `{TSH}` is what the user types
and what the model sees; the stored form holds the attribute, so a rename changes how the comment reads without the
comment being edited, and a deserialised variable is re-pointed at the KB's attribute on load.

`CommentTemplate.render(case)` is a pure function shared by server and client. It substitutes each attribute's latest
value and, where there is none, inserts `{TSH: no value}` and records the character range, so the Comments panel can
highlight it. A comment with no variables renders verbatim.

The chat instructions have the model find each `{name}`, match it to an attribute (exact match binds; otherwise it
asks), and emit a `variables` array with the action. The server then resolves the names to ids and aligns the array to
the number of placeholders actually present; a comment with no placeholders carries no variables however the model
reported it.

## Showing conditions in the user's words

A reason is typed as `elevated glucose` and stored as `Glucose is high`. The expert re-reads rules far more often than
they write them, thinks in their own vocabulary, and may not write English, so the phrase is kept and shown wherever the
condition is shown — comment and derived-value tooltips, the cornerstone tooltip, rule summaries — with the formal text
beneath in smaller grey. The formal text is never omitted: it is the ground truth, and the phrase may be stale after an
attribute rename or may have been mistranslated. When the phrase is blank or equals the formal text (suggested and
edited conditions), only the formal text appears.

**The phrase belongs to the condition, not the rule.** A condition is stored once and shared, so one phrase per
condition keeps every rule reading the same way. When two phrases reach the same condition the **first wins**:
last-write-wins would silently change rules the user has reviewed, and a list of synonyms has nothing sensible to
display. The user is told when their wording differs from the stored phrase (`you previously called this 'elevated
glucose'`), which is deterministic and asks nothing, and can rename the phrase (`RenameCondition`), which identifies the
condition by formal text or current phrase and replaces the phrase by id in every rule and in the active session.

Only the formal text is ever supplied to the model: phrases are display metadata, and feeding them back would present
the model with predicates it did not define.
