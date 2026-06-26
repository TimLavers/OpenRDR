# Inserting variables into a comment

## What do we want to achieve?

We want a comment to be able to include the current value of a case attribute, so that the rendered report reflects
the actual data of the case. The user writes a variable by delimiting an attribute name with braces, e.g. `{TSH}`.

For example, the comment `Your patient has a TSH of {TSH} mIU/L.` renders, for a case whose latest `TSH` value is
`8.2`, as `Your patient has a TSH of 8.2 mIU/L.`.

## User-facing syntax

- A variable is an attribute name in braces, e.g. `{TSH}`.
- Variables are bound to attributes **by order of appearance** in the comment text.
- Since the UI is chat based, the user adds such a comment through the chat (see `building_a_rule_using_the_chat.md`).

## How the chat binds variables

The prompt section `server/.../chat/instructions/4_comment_variables.md` instructs the model to:

1. find each `{name}` placeholder (in order),
2. match the braced name against the attributes in the `ATTRIBUTES` prompt variable (case-insensitive, tolerant of
   small misspellings),
3. auto-bind on an exact match, otherwise ask the user which attribute is meant, and
4. emit the add-comment action with a `variables` array, one entry per placeholder, each carrying an `attributeName`.

The chat-level variable is `ChatCommentVariable(attributeName)`. `resolveCommentVariables(...)` then:

- replaces every `{...}` placeholder with the internal `VARIABLE_TOKEN` (`${}`),
- aligns the variables to the number of placeholders actually present (extra variables are dropped, so a comment with
  no placeholders carries no variables), and
- resolves each `attributeName` to an attribute id via `RuleService.attributeForName`, falling back to the sentinel
  `UNRESOLVED_ATTRIBUTE_ID` (`-1`) when the name cannot be resolved.

## Internal representation

A `Conclusion` (in `common`) stores:

- `text` — the comment with each variable replaced by the `VARIABLE_TOKEN` (`${}`), and
- `variables: List<CommentVariable>` — one `CommentVariable(attributeId)` per token, in order of appearance.

An empty `variables` list means a plain comment, so existing comments remain backward-compatible.

## Rendering

`Conclusion.render(case, attributeById): RenderedComment` is a pure function shared by the UI and the server:

- it walks the `${}` tokens in order, substituting each with the attribute's latest value for the case
  (`RDRCase.latestValue`);
- if the attribute is unknown, or has no value (or a blank value) for the case, it inserts a visible marker
  (e.g. `{TSH: no value}` or `{no value}`) and records the marker's character range in
  `RenderedComment.unresolvedRanges` so the UI can highlight it distinctly.

A comment with no variables renders as its text verbatim with no unresolved ranges.

## Discoverability

The facility is surfaced to the user in two ways, without repeated nagging:

1. it is included in the chatbot's capabilities summary (`16_listing_capabilities.md`), and
2. a one-line tip is shown **once per chat session** the first time the user adds a comment, suggesting they can embed
   case values with braces, e.g. `{TSH}`.
