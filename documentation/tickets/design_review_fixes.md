# Design review fixes — working list

Branch `display-comments-as-a-list`. Findings from the review of the 30 commits from `ee5f0eab`, worst first, with the
agreed order of work. Update the status as each is finished.

## Done

- **Cuke blocker.** `And pause` (a one-day sleep) and a stray `@single` removed from
  `cucumber/src/test/resources/requirements/chat/Naming and renaming.feature`. The pause was deliberate, to reproduce
  the comment-suggestion problem below.
- **Comment attributes were offered a value condition.** `C1 is "Let's surf."` merely restates that the case was given
  the comment, and stops holding once the comment has a variable. Comment attributes are now excluded from the episodic
  factories and offered `C1 is in case` instead (`ConditionSuggester.commentAttributeSuggestions`). Presence only, not
  absence: a KB has one comment attribute per comment text, so nearly all are absent from any one case. Documented under
  "Attributes the knowledge base assigns itself" in `documentation/design/targeted_suggested_conditions.md`. *Commit:
  `suggest a presence condition for a comment attribute, not a value condition`.*
- **Correctness 1: a caret expression was not treated as a formula.** `^` was missing from
  `RuleSessionManager.valueExpressionFor`'s operator set, so `height ^ 2` became a literal — while
  `DERIVED_ATTRIBUTES_HELP_TEXT` tells the user to write exponentiation that way. The review's suggested fix (delegate
  to
  `common`'s `parseValueExpression`) was **wrong**: the two functions keep plain text literal by different mechanisms,
  and delegating with the creating lookup would have made `diabetic` a formula. One-line fix to the regex. *Commit:
  `treat a caret expression as a formula, as the help text says it is`.*
- **A formula could invent the attributes it references.** Found while fixing the above: the lookup passed to
  `FormulaParser` was `getOrCreate`, so `non-diabetic` became `Formula(non - diabetic)` plus two junk attributes, and a
  typo like `weight / hieght` silently produced a formula that could never evaluate. Names are now resolved with
  `attributeForName` and nothing is created; a name that resolves to nothing is put back to the user
  (`didYouMeanFormulaMessage` / `unknownAttributeInFormulaMessage`), which reaches them through the
  `catch (IllegalStateException) → ChatResponse(e.message)` both chat actions already have. Rule documented in
  `documentation/design/repeat_inferencing.md`, "Value expressions". *Commit:
  `refuse a formula that names an attribute the KB does not have, and ask what was meant`.*

  Consequence accepted: a formula can no longer be defined before the KB has seen the attributes it references. Two
  fixtures (`bmiDefinedByRule` in `RenameAttributeTest` and `EditDerivedDefinitionTest`) relied on that and now create
  the attributes. The test
  `an arithmetic expression referencing a missing attribute is a formula that evaluates to null`
  was deleted: it pinned exactly the behaviour we decided against.

- **Correctness 2: a refused comment session left a stale pending change.** `startAssignmentSession` put `currentChange`
  back when `startRuleSession` threw, but the three comment entry points set
  `currentChange`/`commentAttributeInSession`/`diffAttribute` and called `startRuleSession` unguarded, so a refused
  request left its preview for the next session to show. All four now go through one `startSession`, which saves the
  preview and restores it if the session is refused.

  Restoring, not clearing, is the point: one of the three ways `startRuleSession` refuses is that another session is
  already in progress, and the fields then belong to *that* session. Clearing them — which is what
  `startAssignmentSession` did — would wreck a running session's preview to tidy up after a request it had just turned
  away. Fixed for the assignment path too, by the same code.

  Four tests in `RuleSessionRefusalTest`, one per reachable path plus a positive control; three of the four failed
  before the change. `startAssignmentSession` now also clears `commentAttributeInSession`, which it never used to:
  during an assignment session `nameOfCommentAttributeInSession()` would otherwise still answer with the comment left
  over from an earlier comment session. *Commit: `put the pending change back when a rule session is refused`.*
- **A refused assignment left the target attribute behind, and blocked the retry.**
  `startRuleSessionToAssignValue` created the attribute *before* parsing the expression, so a refused request left a
  derived attribute with no rule and no definition. Worse, `AssignDerivedValue` then refused the retry with "A derived
  attribute named "bmi" already exists" — so the "Did you mean …?" question added above could be asked but not answered.
  Everything that can refuse the request now runs first, and the attribute is created only once none of it has fired.

  The self-reference case is what made this look harder than it was: with the attribute not yet created, `BMI * 2` for a
  new `BMI` no longer resolves, and would have been reported as an unknown name instead of a cycle. It does not need a
  graph, though — an expression naming the attribute it defines is circular by inspection — so `valueExpressionFor`
  takes the name being defined and refuses it directly, wording it through a new `cycleMessageForNames` so the sentence
  stays in one place.

  *Two claims I made on this ticket were wrong.* The cycle tests do **not** both pre-create `BMI`:
  `AssignValueDefinitionFlowTest.a self-referencing expression is refused before anything is stored` asserted
  `byName("BMI")!!`, i.e. it pinned the leak, under a name that claimed the opposite. That assertion now asserts what
  the test is called. And no delete API was needed. `RuleSessionManagerAssignmentTest`'s version does pre-create `BMI`,
  and is the regression guard for the existing-attribute path. *Commit:
  `validate an assignment before creating the attribute it assigns`.*
- **The chat instructions answered BMI requests with their own example formula.** Found by running the new cukes: asked
  for `weight/hieght^2`, the model emitted `weight / (height * height)` — character-identical to the BMI example in the
  instructions, spaces and all, while the user's text had no spaces and a caret. Not a correction but a substitution,
  and it would have produced the same formula for `weight/age^2`. It also discarded the `^` the previous fix had just
  taught the server to honour, so `DERIVED_ATTRIBUTES_HELP_TEXT` was telling users to write something the model undid.

  The "BMI" ↔ `weight / (height * height)` pairing appeared five times across `16_listing_capabilities.md`,
  `17_assigning_derived_values.md` and `18_editing_derived_definition.md`; the examples now use `Pulse pressure` /
  `systolic - diastolic`, and step 3 states that the expression is transcribed as the user wrote it. Step 5 covered the
  three refusals but never said what to do when the server asks a *question*, so nothing told the model to re-issue the
  action once the user accepts a correction — added.

  Agreed policy: the model transcribes, the server decides. Recorded under "Value expressions" in
  `documentation/design/repeat_inferencing.md`. I wrote here that tolerance was merely moved to `attributeForName`'s
  silent one-edit band; the next finding below shows that band had no business applying to a formula either. *Commit:
  `have the model transcribe a value expression instead of supplying its own`.*

  The first run of this found the instruction had been written twice over, and contradicted itself: step 3 forbade
  correcting a name, step 5 required sending the server's correction once the user accepts. The model obeyed step 3, so
  "yes" re-sent `weight/hieght^2`, fetched the same question, and the two looped. The exception now sits beside the
  prohibition in step 3 rather than sixty lines below it.
- **A formula bound `height` to `weight`.** The same run failed `A formula referencing an attribute with no value makes
  no assignment` with `0.01075`, which is `1/93`: the formula had become `weight / (weight * weight)`. Names in a
  formula were resolved by `attributeForName`, which tolerates one edit — and `levenshtein("weight", "height")` is 1,
  since only the first character differs. A regression from *refuse a formula that names an attribute the KB does not
  have*, which swapped `getOrCreate` for `attributeForName`; before it, the missing `height` was created unvalued and
  the formula simply evaluated to nothing.

  Formula names now resolve exactly, by `attributeNamedExactly` — case and punctuation only, no edit distance. The
  tolerance that suits a name mentioned in passing is wrong for a definition that is stored, applied to every later
  case, and never shown again. Edit distance survives only in `nearestAttributeName`, which asks rather than decides.
  The test
  `a small misspelling in a formula is corrected without asking` pinned the behaviour that caused this, so it is
  inverted, not deleted.

  Found while fixing it: the reading was also order-dependent. `TokenParser` stops at the first name it cannot resolve,
  so
  `resolvedAName` was only ever true if a resolvable name came first — `weight / age` asked the user, while
  `age / weight`
  became the literal text. `namesInFormula` in `common` now tokenises the expression independently of the parse, and the
  mutable state in the lookup closure is gone.

  Two cukes changed as a consequence. The one added an hour earlier for the silent band went, its subject having gone;
  and the Dirac scenario never established `height` as an attribute at all, so it was testing an unknown name rather
  than an attribute the case has no value for — a second case now supplies it. *Commit:
  `resolve a formula's names exactly, and independently of the parse`.*
  **Still needs a real cuke run: the prompt half of this cannot be verified from unit tests.**
- **Correctness 3: a replacement preview was lost when the comment had variables.** `Diff.attributeName` held the
  replacing attribute's name, so `CommentRows` could not use it to identify the row being replaced and fell back to
  matching text. That fails for a variable comment because the pending change carries its template while the row carries
  its value rendered for the case. `Replacement` now carries `replacedAttributeName` as well, `RuleSessionManager` keeps
  both attribute references current across renames and refused sessions, and `CommentRows` matches the outgoing row by
  its attribute name. Text matching remains only as compatibility for older unnamed diffs.

  Unit coverage proves that identity wins even when a different row happens to match the template text, that both names
  survive serialization, that an older payload defaults the new field, and that a refused request restores both names of
  the running replacement preview. A dedicated cuke, `A replacement preview should not be lost when the comment being
  replaced has a variable`, passed with 17 steps. *Implementation originally landed in commit `Ensure that the variable placeholders are shown in the preview of a
  comment being added or a replacement comment, rather then the evaluated text`.*
- **The pending-addition row could land in the wrong place.** The row was always appended on the assumption that a
  comment being added always had a newly created attribute with the highest id. `commentAttributeFor` can instead reuse
  an older comment attribute when the text already exists in the KB, so committing the rule made the row jump to that
  attribute's earlier position.

  `RenderedComment` and `Addition` now carry the comment attribute id, and `InterpretationViewManager` and
  `RuleSessionManager` populate it. `CommentRows` inserts a pending addition in attribute-id order when the complete
  ordering information is available; payloads from older servers have no ids and retain the previous append behaviour.
  Coverage proves an existing `C2` is previewed between `C1` and `C3`, that reuse sends the existing id, and that both
  new fields survive serialization. A dedicated cuke, `A pending existing comment should be previewed in comment
  attribute order`, passed with 22 steps.
- **A rejected import left a half-built KB.** The legacy-conclusions guard now runs immediately after reading the
  export's KB details and before creating any persistent KB. A rejected import therefore leaves no KB id, metadata,
  attributes or ordering behind. The `Conclusions` directory now has a `KBExportImport.conclusionsDirectory` property
  alongside every other export directory. A focused importer test proves the error is unchanged and persistence remains
  empty.

- **Dangling implementation-plan references.** References to the deleted numbered phases, steps and decisions have been
  removed from requirements, design notes, KDocs, test helpers and feature comments. Each useful explanation now states
  the behaviour directly instead of pointing to a missing plan entry.
- **The ordering documentation contradicted the implementation.** `repeat_inferencing.md`,
  `comment_naming_and_attribute_ordering.md`, the milestone requirements and `InterpretationViewManager` now agree:
  comments are currently sorted by attribute id, derived values by attribute name, and persisted user-controlled
  ordering is not implemented. The misleading Cucumber feature is now titled `Comments should be shown in a
  deterministic order`.
- **The attribute-name requirements contradicted validation.** `attributes.md` now distinguishes exact-name uniqueness,
  case-sensitive external names, and case-insensitive uniqueness for KB-assigned names. It also records the real
  boundary: construction refuses an empty name but currently permits whitespace-only names, while renaming trims and
  refuses blank input.
- **The milestone requirements described the retired interpretation view.** The old plain monospaced text and conclusion
  ordering have been replaced by the current two-column Comments panel, with its deterministic but non-user-controlled
  order.
- **Smaller documentation defects.** The duplicated `currently formerly` wording is fixed, and `Derived attributes` is
  now the shared `DERIVED_ATTRIBUTES_LABEL` constant rather than the panel's sole hardcoded UI string. A focused UI test
  verifies the heading.
- **Dead code.** `AttributeManager.isNameInUse`, `MAXIMUM_COMMENT_LENGTH`, and the no-op
  `checkRuleSessionHistoryConsistency()` call and function have been removed. The unused `parseValueExpression` policy
  wrapper has also gone; it could not safely become the single parser because it silently made an unresolved formula a
  literal, while the live `RuleSessionManager` policy must ask the user what they meant. Its useful arithmetic and caret
  coverage now exercises the production-used `FormulaParser` directly, and the wrapper-only literal tests were removed.
  The comment requirements still record 2,048 characters as the wanted but unenforced maximum without claiming that a
  dead constant implements it.
- **Load-time attribute alignment no longer hides inconsistent persistence.** `ConditionManager` was already aligning
  directly; the `runCatching { … }.getOrDefault(…)` wrappers have now been removed from `RuleManager` and
  `DerivedDefinitionManager` as well. A stored rule or definition referring to an attribute id the KB does not hold now
  prevents the KB from loading, with regression coverage for both paths.

  This left `:server:test` red in six places, all in `RuleManagerTest`, and all the fixture's fault rather than the
  production change's. Its comments came from the `CommentFactory` test helper, which mints a comment attribute without
  telling the attribute manager, so every rebuild of the tree then failed to find the attribute — exactly the
  inconsistency the removal is meant to expose, manufactured by the test. The comments now come from
  `attributeManager.createCommentAttribute()`. The new regression test failed for a second reason: its unknown attribute
  was `EXTERNAL`, which `AssignValue` refuses before the manager is ever consulted, so it threw
  `IllegalArgumentException` and never reached the behaviour it was written for. The attribute is now `COMMENT`.
- **`ActionComment` has one value-expression field.** The unused `replacementValueExpression` field and the mapping that
  let it overwrite `valueExpression` have been removed. Derived-value assignment, replacement and definition editing all
  use the documented `valueExpression` field.
- **`ViewableCase` cannot validate more than the case id — reverted.** The constructor was changed to compare the rule
  summaries as well, on the grounds that `Interpretation` equality is `caseId`-only. That invariant does not hold:
  `KB.viewableCase` passes `withResolvedDefinitions(...)`, a display copy in which every `ByDefinition` assignment is
  replaced by the attribute's stored definition, so the summaries differ by design. The check therefore fired on every
  comment rule, `POST /api/buildRule` returned 500, and all 30 chat cukes failed in their setup step. `:common:test` was
  red as well, with four `ViewableCaseTest` fixtures refused and the new regression test throwing
  `IllegalArgumentException` from `AssignValue`, whose attribute must be KB-assigned — so it never exercised the check
  at all.

  Comparing only the rule *ids* would survive display resolution, but it buys no known defect and turns any future
  display inconsistency into a dead client rather than a slightly wrong panel. The check is back to case identity, and
  the review finding is withdrawn: the viewable interpretation is a rewritten copy of the case's own, so nothing
  stronger than the case id is a property of both.
- **`RuleSessionManager` contains no `!!`.** Session-dependent operations now obtain one checked `RuleBuildingSession`
  local, condition and case ids are explicitly required, and undo reports when there is no recorded session.
- **Stale conclusion-era test names are gone.** The three script suites and files are now `AddingCommentsTest`,
  `RemovingCommentsTest`, and `ReplacingCommentsTest`, and their test names use comment terminology.
- **Finished ticket artifacts are absent.** Neither `step16_test_conversion_plan.md` nor
  `case_selector_selection_desync.md` exists in the worktree or Git index, so no deletion was necessary.
- **Folder Cucumber retries are intentional.** The suggestion to fail a folder task on the first failed scenario was
  rejected: GUI and LLM cukes can fail transiently, and project policy counts a failed scenario as passing when its
  immediate rerun succeeds. The first failure and the scenarios being retried remain visible in the task output.

## Remaining, in order

None.

## Also raised, not yet scheduled

None.

## Verification

Unit tests after each step, from the repo root:

```
.\gradlew.bat :common:cleanTest :common:test :server:cleanTest :server:test --tests "io.rippledown.kb.*" ^
  --tests "io.rippledown.model.*" --tests "io.rippledown.server.*" --tests "io.rippledown.suggestions.*" ^
  --tests "io.rippledown.util.*" --tests "io.rippledown.persistence.inmemory.*" --console=plain
```

Last run, after the `ViewableCase` revert and the `RuleManagerTest` fixture fix: `:common:test`, `:server:test` (1290
tests over the non-Postgres packages) and `:ui:test` all green.

Focused verification after the dead-code removal: `ValueExpressionTest` passed and `:server:compileKotlin` succeeded.

`.\gradlew.bat :cucumber:chat`: 38 scenarios, 721 steps, all passed, in 10m 8s. This is the run that had failed 30 of 30
in its setup step on the `ViewableCase` check.

`.\gradlew.bat :cucumber:conditions`: all passed, after a fix to `InterpretationPO.movePointerOverCentreOf`. `Some
suggested conditions can be modified before being added` had failed twice over, waiting the full ten seconds for the
condition tooltip. The rule itself had committed and the comment was on screen, so only the hover had failed: a
`Robot.mouseMove` to where the pointer already is generates no event, and since the window occupies the same screen
position in every scenario, the scenario before can leave the pointer on the very pixel being moved to — whereupon
neither the first attempt nor any retry of it moves anything. The pointer now always arrives from somewhere else.

The cuke `The user should be able to accept the chatbot's correction of a misspelt attribute name` now covers the new
"Did you mean …?" confirmation and the accepted corrected formula. It still needs a real LLM-and-GUI cuke run by the
user.
