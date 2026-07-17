# Repeat Inferencing via Derived Attributes

## Motivation

A user may build a rule that establishes that a patient is diabetic, and then
want to write further rules whose conditions depend on that diagnosis, for
example a rule giving dietary advice for diabetic patients. Rather than
forcing the user to repeat the raw-data conditions that established the
diagnosis, we allow rules to record intermediate findings on the case, and
other rules to use those findings in their conditions.

This requires the inference engine to run repeatedly: a finding recorded in
one pass can trigger further rules in the next pass, until the
interpretation is stable.

## The model: derived attributes

In production, a case originates from an external information system — for
example a laboratory information system (LIS) — which assigns its
attributes and values. These are called *external attributes*. We model the
action of the knowledge base as *adding more attributes to the case*, whose
values are assigned by the knowledge base rather than the external system.
These are called *derived attributes*.

The value of a derived attributes is always assigned by a rule. The reason for this is explained below.

Once a derived attribute has been assigned a value and added to the case, it
is indistinguishable from an externally assigned attribute for the purposes of
condition evaluation, with one minor difference: the knowledge base only
assigns values in the most recent episode of the case.

There is exactly one kind of rule action: *assign the result of a value
expression to a derived attribute*, e.g.:

- `Diabetes status = "diabetic"`
- `Risk score = 7`
- `BMI = weight / (height * height)`

Both the attribute (user-named at rule-building time) and the value
expression are chosen by the user.

### Value expressions

There are, in effect, two kinds of derived attribute:

- **rules-based** concepts like `Diabetes status`: conditions plus a
  *literal* value;
- **formula-based** quantities like BMI or creatinine clearance ratio:
  no conditions (they apply to every case) plus a *computed* value.

These are unified rather than modelled as separate types: the assigned
value is a *value expression*, of which a literal is the trivial case. A
formula-based derived attribute is exactly a rules-based one with an empty
condition set and a non-trivial expression. No attribute-level distinction
is needed; both are derived attributes.

This unification has significant benefits:

- Fixpoint inference, stratification, reset semantics and KB-ownership
  apply unchanged. An expression referencing another derived attribute
  resolves on a later pass; expression references contribute edges to the
  dependency graph exactly as conditions do.
- Refinement comes for free: the leaf-most-rule-wins structure means a
  formula can be overridden by a conditioned child rule — e.g. a corrected
  BMI formula for amputees. RDR is exactly the right machinery for such
  exceptions.
- It closes the known gap noted in [conditions.md](conditions.md) ("Other
  kinds of conditions"): calculations like `mass/(height * height) > 28`
  become expressible as a condition on a formula-derived attribute.

Evaluation semantics: if any attribute referenced by the expression has no
value in the case, no assignment is made — the derived attribute is simply
absent from that case. This keeps `is in case` conditions on formula outputs meaningful
downstream, and means formulas need no guard conditions.

There are two other reasons why a derived attribute can be absent from a case:

1. the rule assigning its value is not satisfied, or
2. there is a rule that removes its value

The expression language is deliberately small at first: arithmetic on the
latest values of attributes (`+ - * /`, parentheses, numeric literals).
Functions, episode indexing and text manipulation can be added later if
needed.

### Comments are derived attributes

Report comments are themselves derived attributes, of a *comment* subtype.
When the user requests

> add the comment "Patient is diabetic"

the system creates a comment attribute, automatically assigns it a name,
and informs the user:

> Please confirm that you want to add the comment "Patient is diabetic".
> I have given this comment the name "C1", but you can change the name at
> any time.

Since the chat LLM sees the comment text, it should propose a semantic
default name (e.g. `DiabetesStatus`) where it can, falling back to `C1`,
`C2`, … Renaming is always safe: rules and conditions reference the
attribute id, not the name.

The report is the set of comment-attribute values present on the case. The
AI report generator receives these named comment attributes as its inputs;
a meaningful name is useful signal about the comment's role. Comment
ordering is no longer significant: the AI produces the report, so the
ordering machinery that mattered under string concatenation is not needed.

The existing comment actions map directly onto assignment:

- *add comment* → assign a value to a new comment attribute
- *remove comment* → refinement rule retracting the assignment
- *replace comment* → child rule assigning a different value to the same
  attribute

Conflicts between rules assigning the same attribute are resolved by the
existing RDR refinement structure: the leaf-most satisfied rule for an
attribute wins, exactly as `Rule.apply` works today for conclusions.

Comment values must continue to support the existing `${}` attribute (or derived attribute)
variable placeholders (see `Interpretation.toComments`).

### Presentation

- Comment attributes appear in the Comments panel, each with its name;
  they do not appear in the case data table.
- Non-comment derived attributes (e.g. `Risk score`, `BMI`) appear in
  their own collapsible panel just under the case view, alongside the
  Comments and Report panels — not in the case data table. This suits
  their non-episodic nature (a single value in the latest episode would
  occupy a mostly empty row of the episodic grid), and makes the
  KB-owned/external distinction structural rather than a matter of
  styling. The panel is hidden when the case has no derived values.
  It is labelled **"Derived values"**: *derived attribute* is the
  model/code term (precise, symmetric with *external attribute*), but
  "attribute" is jargon to clinical users, who think in results and
  values.
- The cornerstone view shows derived attributes the same way: during rule
  building, a chained rule's effect on a cornerstone will often be visible
  only in its derived values.

### Conditions — no new machinery

Because derived attributes are ordinary attributes, the entire existing
condition syntax ([conditions.md](conditions.md)) applies to them
unchanged:

- `Diabetes status is in case` / `is not in case`
- `Diabetes status is "diabetic"`, `Diabetes status contains "diab"`
- `Risk score > 10` and all other numerical conditions

No new condition class, parser extension, suggestion type, serialization,
or id-alignment work is needed: conditions reference derived attributes by
attribute id, and the existing `alignAttributes` mechanism handles KB
import/export.

## Inference algorithm

`RuleTree.apply` currently makes a single depth-first pass. This changes to
a fixpoint iteration, applied wherever a case is interpreted (`KB.interpret`,
including cornerstone evaluation during rule building):

1. Strip all derived-attribute values from the case (see reset semantics
   below).
2. Evaluate the tree against the case; collect the derived-attribute
   assignments made by the rules that fired.
3. Write those assignments into the latest episode of the case.
4. If the assignments (and conclusions) are unchanged from the previous
   pass, stop. Otherwise repeat from step 2, with no hard cap of
   passes as this is guaranteed to terminate.

Conditions on external data evaluate identically on every pass; only conditions
on derived attributes can change value between passes. Because the
dependency graph is kept acyclic (next section), the iteration is
guaranteed to converge.

### Reset semantics

Re-interpreting a case must be idempotent. `resetInterpretation` (and any
other path into inference) must remove all derived-attribute values from
the case before the first pass, so that stale values from a previous
interpretation cannot influence the new one.

## Stratification: keeping dependencies acyclic

Negative conditions on derived attributes (`X is not in case`) make naive
fixpoint iteration unsafe: a rule assigning A when B is absent, and a rule
assigning B when A is absent, oscillate forever. We prevent this at
rule-build time.

### Dependency graph

- Nodes: derived attributes.
- Edges: derived attribute B *depends on* derived attribute A if some rule
  whose action assigns B (or removes/replaces an assignment of B) has a
  condition referring to A anywhere on its path from the root, or has a
  value expression referring to A.

Because conditions reference attributes by id, and the set of derived
attributes is known, edge extraction from the rule tree is exact.

### Build-time prevention

Cycles are prevented during rule building, before they can reach the rule.
A condition on a derived attribute *would create a cycle* if adding its
would-be edges to the graph makes the graph cyclic. Such conditions are
kept away from the user at every entry point:

- **Suggestions**: the condition suggester never offers a condition that
  would create a cycle — cycle-creating candidates are filtered out before
  ranking, so the user simply doesn't see them.
- **Manual entry**: if the user enters such a condition (typed expression
  or edited suggestion), it is not added to the rule, and the chat
  explains why, identifying the cycle, e.g.:

  > This condition cannot be used: it would make "X" depend on itself
  > (X → Y → X).

Therefore, there is no need for a cycle check when the rule is committed.

## Derived attributes are KB-owned

- Derived attributes are flagged as KB-assigned, distinguishing them from
  external attributes.
- The external system cannot supply values for them. If an incoming case
  has an external attribute whose name matches a derived attribute, the
  external attribute's name is mangled deterministically (e.g. `A` →
  `A (external)`) — case processing must never fail and external data must
  never be silently dropped, while the derived attribute keeps the name
  the user gave it. The same external name maps to the same mangled
  attribute on every case, so its values stay together. Collisions are
  logged. The mangled name is not permanent: attribute renaming will
  (eventually) extend to external attributes, so the user can give the
  mangled attribute a better name — renaming is id-safe here as
  everywhere else. One requirement this creates: external attributes are
  matched by name at case ingestion, so a renamed external attribute
  needs a persisted alias mapping its original external name to the
  attribute — otherwise the next incoming case would recreate the
  collision and split the data across two attributes.
- Conversely, when the user names or renames a derived attribute, a name
  already in use (external or derived) is refused in the chat — prevention
  rather than mangling, since a user is present to choose another name.
- Derived values are interpretation artefacts: they are never echoed back
  to the external system, and are excluded from whatever is exported as
  case data.
- Neither kind of derived attribute appears in the case data table:
  comment attributes are shown in the Comments panel, and other derived
  attributes in the Derived attributes panel (see Presentation).

## Rule building

- **Cornerstones**: cornerstone cases are interpreted with the iterative
  algorithm, so a proposed rule's effect on a cornerstone is judged after
  the fixpoint is reached. A new rule may change a cornerstone's
  interpretation indirectly (via a chain of derived attributes); this is
  detected naturally since cornerstone comparison already compares whole
  interpretations.
- **Condition suggestions**: the suggester offers conditions on derived
  attributes present in the current case, and (lower-ranked) presence/
  absence and value conditions on other derived attributes in the KB.
  Conditions that would create a dependency cycle are never offered.
- **User expressions**: no parser changes needed — conditions on derived
  attributes use the existing syntax.

## Resolved design decisions

1. **Comments are derived attributes** (see above). One rule action kind
   for the whole system: assign a value to a derived attribute.
2. **Refinement semantics**: remove = retract assignment; replace = child
   rule assigning a different value to the same attribute.
3. **Conflict resolution**: leaf-most satisfied rule per attribute wins,
   as per the existing RDR refinement structure.
4. **Comment ordering**: not significant — the AI report generator
   consumes the named comment attributes, so the ordering machinery that
   mattered under string concatenation is unnecessary.
5. **Derived attribute ordering**: same process as for external attribute ordering. A new derived attribute will be last
   on the list, but the user can change this.
6. **Episodic derived attributes**: deferred. Scenarios where derived
   values in earlier episodes would be useful are imaginable (e.g.
   conditions like `previous Diabetes status is "diabetic"`, or trends in
   a derived quantity such as BMI), and the model leaves room for them as
   derived values are ordinary values in episodes. Not for the first implementation however.
7. **Conclusions as derived attributes**: Comments-as-conclusions run deep
   (`Conclusion`, `RuleSummary`, interpretation diffs, the conclusion
   store, conclusion ordering). Existing KBs need each conclusion
   converted to a comment attribute plus assignment.
8. **Attribute creation UX**: The user adds a non-comment derived attribute by naming it and typing it as text or
   numeric. Within the scope of this rule session, they cannot add another derived attribute.

## Out of scope

- Grouping derived attributes into folders.
- Assigning the value of a derived attribute using an AI rather than with a rule ("please read the clinical notes and
  assign diabetic status to be "diabetes" if indicated")
- Referring to derived attributes from *other* knowledge bases.
- Historical derived values in earlier episodes (see resolved decision 5).
- Conditions on report structure (ordering, sections).

## Implementation plan

The plan is phased: derived data attributes and repeat inferencing first, with
comments untouched; comments are recast as derived attributes in a second
phase. Each step follows TDD: tests are written before the production code
they cover, and each step leaves all tests green.

### Status (as of this revision)

**Done:** Phase 0 (steps 1–3) and Phase 1 steps 4–7. **Remaining:**
Phase 1 steps 8–9, then Phases 2–4. The completed steps below have been
compressed to implementation notes so that the remaining steps can refer
to real, existing symbols.

### Conventions (mandatory for all remaining steps)

- **TDD**: write the tests for a step before its production code. Test
  bodies use `// Given` / `// When` / `// Then` comments. Kotest matchers
  (`shouldBe`, `shouldThrow`) with `kotlin.test.Test`.
- **Never use `!!` in production code.** Use `requireNotNull(x) { "…" }`,
  `checkNotNull(x) { "…" }`, `?:` with `error("…")`, or safe calls. `!!`
  is acceptable in test code only.
- **No in-code DB migrations.** New columns go into the Exposed table
  definitions only (`SchemaUtils.create` covers fresh databases). When a
  schema changes, state the one-off `ALTER TABLE` SQL in the commit/PR
  description for the user to run manually against existing KB databases.
- **Do not delete or weaken existing tests.** If a behaviour genuinely
  changes, update the test and say why.
- Run `.\gradlew.bat :common:test :server:test` after each step;
- Run specific cucumber scenarios after each implementation step using @single annotation on each scenario to be tested.
  Then use `.\gradlew.bat cucumberSingleTest`.

### Phase 0 — groundwork (DONE)

1. **Attribute kind.** `Attribute` now has `kind: AttributeKind`
   enum values: `EXTERNAL`, `DERIVED`, `COMMENT`; `AttributeKind.isAssignedByKB()` is
   true for the latter two. `AttributeManager.getOrCreate(name, kind)`
   exists; the Postgres attributes table has a `kind VARCHAR(32)` column
   (one-off migration for existing DBs:
   `ALTER TABLE attributes ADD COLUMN IF NOT EXISTS kind VARCHAR(32) NOT NULL DEFAULT 'EXTERNAL';`).
2. **Case support.** `RDRCase.withDerivedValue(attribute, value)` and
   `RDRCase.withoutDerivedValues()` return copies carrying over case id
   and interpretation.
3. **External collision guard.** In `KB`
   (`server/src/main/kotlin/io/rippledown/kb/KB.kt`): external values for
   a name owned by a derived attribute go to a mangled
   `"<name> (external)"` attribute, created once and reused; collisions
   logged.

### Phase 1 steps 4–7 — actions, fixpoint, sessions, cycles (DONE)

Key symbols, all covered by tests:

- `RuleAction` / `GiveConclusion` / `AssignValue(attribute, expression)`
  in `common/.../model/rule/RuleAction.kt`. `AssignValue.evaluate(case)`
  returns null (no assignment) if the expression cannot be evaluated.
- `ValueExpression` (`Literal`, `Formula`), the arithmetic tree `Expr`
  (`Num`, `AttributeValue`, `Binary`, `Operator`), and
  `FormulaParser((String) -> Attribute?)` in
  `common/.../model/rule/ValueExpression.kt`.
- `Rule.assignment: AssignValue?` (a rule has a conclusion or an
  assignment, never both); `Rule.action: RuleAction?`.
- `Interpretation.assignments(): Set<AssignValue>`.
- Fixpoint: `RuleTree.materialise(case)` (server `RuleTree.kt`) strips
  derived values then iterates to a fixpoint, returning the case copy
  with derived values written; `RuleTree.apply(case)` delegates to it.
  `KB.interpret` funnels all interpretation through this.
- Tree changes: `ChangeTreeToAddAssignment` / `ChangeTreeToRemoveAssignment`
  / `ChangeTreeToReplaceAssignment` in server `RuleTreeChange.kt`, each
  with `assignedAttribute()` and `expressionReferences()`; changers in
  `RuleTreeChanger.kt`; `RuleFactory.createRuleAndAddToParent(parent,
  assignment, conditions)` overload.
- Persistence: `PersistentRule.assignment: AssignValue?` (JSON-serialized
  into a nullable `assignment TEXT` column in `PostgresRuleStore`;
  one-off migration:
  `ALTER TABLE rules ADD COLUMN IF NOT EXISTS assignment TEXT NULL;`).
- Session entry points on `RuleSessionManager`
  (`server/src/main/kotlin/io/rippledown/kb/RuleSessionManager.kt`):
  `startRuleSessionToAssignValue(case, attributeName, expressionText)`
  (creates the `DERIVED` attribute if needed),
  `startRuleSessionToRemoveAssignment(case, attributeName)`,
  `startRuleSessionToReplaceAssignment(case, attributeName, replacementExpressionText)`.
  `valueExpressionFor(text)` maps a double-quoted string to a `Literal`,
  text parseable as arithmetic over known attribute names to a `Formula`,
  and anything else to a `Literal`.
- Cycle prevention: `DerivedAttributeDependencyGraph(ruleTree,
  knownAttributes)` with `cycleCreatedBy(action, condition)` and
  `cycleCreatedBy(assigned, referenced)`, plus the top-level
  `cycleMessage(cycle)` helper, in server
  `model/rule/DerivedAttributeDependencyGraph.kt`. Guards are in place in
  `RuleSessionManager.startRuleSession` (expression self-reference),
  `addConditionToCurrentRuleSession` (throws `IllegalArgumentException`
  with the cycle message), `conditionForExpression` (returns
  `ConditionParsingResult(errorMessage = …)`), `commitCurrentRuleSession`
  (invariant `check`), and `ConditionSuggester.pruneCycleCreating`.

Existing tests to keep green (they pin the behaviour the remaining steps
build on): `RuleActionTest`, `ValueExpressionTest`, `RuleTreeTest`,
`AssignmentRuleTreeChangeTest`, `RuleManagerAssignmentTest`,
`PersistentRuleAssignmentTest`, `RuleSessionManagerAssignmentTest`,
`DerivedAttributeDependencyGraphTest`, `ConditionSuggesterCycleTest`.

### Phase 1 step 8 — suggestions and chat (REMAINING)

The chat flow asks for or proposes the attribute
name; values are typed by example (numeric if the value parses as a
number). Split into four independently testable sub-steps.

**8a. Materialise the session case for suggestions and typed conditions.**
Problem: `RuleSessionManager.conditionHintsForCase(case)` and
`conditionForExpression(case, expression)` receive the *raw* case, so a
derived value assigned by an existing rule is invisible — no suggestions
are generated for it, and a typed condition on it is rejected by the
"attribute not in case" guard in `conditionForExpression`.

- In `conditionHintsForCase`, build the `SuggestionContext` with
  `sessionCase = kb.ruleTree.materialise(case)` instead of `case`.
- In `conditionForExpression(case, expression)`, evaluate the guard and
  `condition.holds(...)` against `kb.ruleTree.materialise(case)`.
- Do NOT materialise inside `ConditionSuggester` itself — it must stay a
  pure function of its context.
- Tests (extend `RuleSessionManagerAssignmentTest`): build an assignment
  rule (`Diabetes status = "diabetic"` when `Glucose ≥ 11`), then during
  a *comment* rule session on a high-glucose case: (i)
  `conditionHintsForCase(rawCase)` contains a suggestion mentioning
  `Diabetes status`; (ii) `conditionForExpression(rawCase, …)` accepts a
  condition on `Diabetes status` (use `setConditionParser` with a stub
  parser, as in the existing cycle test).

**8b. Suggestions for KB derived attributes not on the case.**
The suggester currently generates only from attributes present on the
session case. Add, in `ConditionSuggester`
(`server/src/main/kotlin/io/rippledown/suggestions/ConditionSuggester.kt`),
a new candidate source `derivedAttributeSuggestions()`: for every
attribute in `ctx.attributes` with `kind == DERIVED` that is *not* in
`sessionCase.attributes`, offer
`CaseStructureCondition(IsPresentInCase(attr))` and
`CaseStructureCondition(IsAbsentFromCase(attr))` as
`NonEditableSuggestedCondition`s, but only those that hold on the session
case (`shouldBeSuggestedForCase`). Include them in `allSuggestions()`
*before* `pruneCycleCreating` so cycle-creating ones are filtered. Ranking:
these should rank *below* current-case candidates — the existing
`RelevanceRanker` scorers give them no boost, which achieves this; do not
add a new scorer. Tests: new cases in `ConditionSuggesterCycleTest` or a
new `ConditionSuggesterDerivedAttributesTest` — absence condition on an
absent derived attribute is offered; presence condition on it is not
(does not hold); cycle-creating ones still filtered.

**8c. Chat actions for assignments.** Mirror the existing comment actions:

- Constants in `common/src/main/kotlin/io/rippledown/constants/chat/Constants.kt`:
  `const val ASSIGN_VALUE = "AssignDerivedValue"`,
  `REMOVE_DERIVED_VALUE = "RemoveDerivedValue"`,
  `REPLACE_DERIVED_VALUE = "ReplaceDerivedValue"`.
- `ActionComment` (`server/src/main/kotlin/io/rippledown/kb/chat/ActionComment.kt`):
  add nullable fields `attributeName: String?` and
  `valueExpression: String?` (and `replacementValueExpression: String?`),
  and put them into `asMap` in `invokeConstructor` alongside the existing
  fields. (The action string is resolved reflectively to a class in
  `io.rippledown.kb.chat.action`, so class names must equal the action
  strings above.)
- New action classes in `server/src/main/kotlin/io/rippledown/kb/chat/action/`,
  modelled directly on `AddComment.kt` / `RemoveComment.kt` /
  `ReplaceComment.kt`: `AssignDerivedValue(attributeName, valueExpression)`,
  `RemoveDerivedValue(attributeName)`,
  `ReplaceDerivedValue(attributeName, valueExpression)`. Each checks
  `ruleService.isRuleSessionActive()` first (return
  `RULE_SESSION_ALREADY_ACTIVE_ERROR` as the comment actions do), calls
  the corresponding `RuleService` method, then
  `ruleService.sendCornerstoneStatus()` and returns
  `modelResponder.response(cornerstoneStatus.summary())`.
- Extend the `RuleService` interface
  (`server/src/main/kotlin/io/rippledown/kb/chat/RuleService.kt`) with
  `startRuleSessionToAssignValue(viewableCase: ViewableCase,
  attributeName: String, expressionText: String): CornerstoneStatus` (and
  remove/replace analogues taking `ViewableCase`). Implement in
  `RuleSessionManager` by delegating to the existing case-level methods
  (`viewableCase.case`). Name-in-use rule: if an attribute with the given
  name exists and its kind is `EXTERNAL`, refuse with
  `error("\"$name\" is the name of an attribute supplied by the external system, so cannot be used for a derived attribute.")`
  — the chat relays the message. (Creating/reusing a `DERIVED` attribute
  of that name is fine and already implemented.)
- Error relay: `startRuleSessionToAssignValue` can throw
  (`IllegalStateException`) for the name-in-use and expression-cycle
  cases. Follow the existing pattern in the comment actions for surfacing
  errors to the chat (see how `ChatAction.doIt` implementations handle
  failures; if they don't catch, wrap the call in try/catch and return
  `modelResponder.response(e.message ?: "…")`).
- Tests: mirror the existing chat action tests (see
  `server/src/test/kotlin/io/rippledown/kb/chat/` for `AddComment`-style
  tests) — happy path, session-already-active, name-in-use refusal,
  cycle-message relay.

**8d. Chat instructions.** Add a new numbered instruction file in
`server/src/main/resources/chat/instructions/` (e.g.
`17_assigning_derived_values.md`, following the style of
`3_defining_the_report_change.md`):

- Describe the user intents: "record/assign a value or finding"
  (e.g. "note that this patient is diabetic as Diabetes status",
  "add a BMI calculation"), removal and replacement of an assigned value.
- The model must obtain an attribute name: if the user gives one, use it;
  otherwise propose a short semantic name and ask for confirmation.
- The value: literal text must be sent wrapped in double quotes
  (`"diabetic"`); numbers and formulas over attribute names sent unquoted
  (`7`, `weight / (height * height)`). The server decides literal vs
  formula (`valueExpressionFor`), so instructions only need the quoting
  rule.
- JSON emitted:
  `{"action": "AssignDerivedValue", "attributeName": "...", "valueExpression": "..."}`
  (and the remove/replace analogues). Reasons (conditions) then follow the
  existing rule-session flow (`6_defining_the_reasons.md` applies
  unchanged).
- If the server refuses (name in use, cycle), relay the server's message
  verbatim and ask the user for an alternative.
- Update `16_listing_capabilities.md` so the capability list mentions
  assigning values to derived attributes.

### Phase 1 step 9 — cucumber (REMAINING)

The feature file already exists and specifies the intended behaviour:
`cucumber/src/test/resources/requirements/inferencing/Repeat inferencing.feature`,
tagged `@ignore`. Implementing this step means adding the missing plumbing
and step definitions, then removing the `@ignore` tag. Scenario-by-scenario
requirements:

- **Backdoor assignment rules.** The existing backdoor
  (`RESTClient.buildRule` → `Api.buildRule` → `KBEndpoint.buildRule` →
  `RuleSessionManager.buildRule(BuildRuleRequest)`) only handles comment
  diffs. Extend `BuildRuleRequest`
  (`common/src/main/kotlin/io/rippledown/model/rule/BuildRuleRequest.kt`)
  with an optional assignment payload — nullable fields
  `assignAttribute: String? = null`, `assignExpression: String? = null`
  (serializable, defaulting to null keeps existing JSON compatible) — OR
  add a parallel `BuildAssignmentRuleRequest` + endpoint; prefer the
  nullable-fields option (less wiring). In
  `RuleSessionManager.buildRule`, when `assignAttribute != null`, call
  `startRuleSessionToAssignValue(case, assignAttribute, assignExpression)`
  instead of the diff dispatch; conditions are parsed exactly as now.
- **Step definitions** in
  `cucumber/src/test/kotlin/steps/BackdoorRuleStepDefs.kt`:
  `@And("a backdoor rule is built for case {word} to assign the value {string} to the derived attribute {string} with conditions:")`
  and
  `@And("a backdoor rule is built for case {word} to assign the formula {string} to the derived attribute {string} with no conditions")`.
  Note the quoting rule: a literal value must be passed to the server
  wrapped in double quotes (`"\"" + value + "\""`); a formula is passed
  as-is.
- **Derived-value assertions.**
  `Then the derived value "X" should be "y"` / `should not be present`:
  until the Phase 3 UI panel exists, assert via REST, not the UI — add a
  `RESTClient` call that fetches the viewable case and inspects the
  materialised values (the server's `viewableCase` is built from the
  interpreted case, which after step 5 carries derived values). If the
  viewable case turns out not to include derived values, add a small
  test-only endpoint returning `attribute name → latest value` for a
  named case, mirroring how other backdoor endpoints are wired in
  `KBEndpoint` and the server routing.
- **Chat scenarios** (`I request that the value … be assigned …`,
  suggestion absence, cycle refusal wording) depend on step 8c/8d. The
  cycle-explanation assertion should check the chat text contains
  `depend on itself` and both attribute names, not the exact sentence.
- Keep each scenario runnable in isolation; remove `@ignore` only when
  all scenarios pass locally.

### Phase 2 — comments become derived attributes

This phase resolves the temporary duplication that Phase 1 introduces
between `GiveConclusion` and `AssignValue` (parallel rule actions,
tree-change and changer classes). A `Conclusion` is really just an
`Attribute` (of `COMMENT` kind) plus a `ValueExpression` (a text template
whose `${}` variables are attribute references), so once comments are
derived attributes, `GiveConclusion` and its parallel machinery are
deleted. The one semantic difference to design for: a report is a *set*
of comments from independent rules, whereas a derived attribute holds a
single value (leaf-most rule wins) — hence one comment attribute per
conclusion rather than one shared "report" attribute.

Phase 2 is large and breaking: steps 10–14 land together behind passing
migration tests before any release. Before starting, re-read this whole
document and survey the current usages of `Conclusion` (it reaches into
`Interpretation`, `RuleSummary`, the diff types, `ConclusionManager`,
`InterpretationViewManager`, the conclusion store and order store, the
chat comment actions, and the UI interpretation package). Sequence the
work bottom-up as below, keeping the build green between sub-steps by
leaving `GiveConclusion` in place until step 14.

10. **Comment attributes.** Each `Conclusion` is replaced by a `COMMENT`
    attribute whose assigned value is the comment text.
    - `AttributeManager` (`server/src/main/kotlin/io/rippledown/kb/AttributeManager.kt`)
      gains comment-attribute support: creation with auto-naming (`C1`,
      `C2`, … — smallest unused index), lookup of all `COMMENT`
      attributes.
    - `${}` comment-variable support moves to the assigned value: the
      rendering currently done via `Conclusion`/`Interpretation.toComments`
      is re-implemented for `Literal` values of `COMMENT` attributes.
      Preserve the existing rendering semantics exactly — the tests in
      the comments cucumber features
      (`cucumber/src/test/resources/requirements/comments/`) pin them.
11. **Migration of configured KBs.** Configured KBs are in zip files under
    `server/src/test/resources`. Convert these to the new format as a one-off
    migration before step 14 lands, and reconfigure them.

- On KB load (and KB import), each existing conclusion
    is converted to a `COMMENT` attribute plus assignment:
    - for each `Conclusion` in the conclusion store, create a `COMMENT`
      attribute (auto-named) whose assigned value is the conclusion text;
    - each rule whose `conclusionId` references it becomes a rule with an
      `AssignValue` on that attribute (update `PersistentRule`:
      `conclusionId` becomes unused);
    - one-way and idempotent: migrated KBs have an empty conclusion
      store, so re-running is a no-op; exports after migration use the
      new form only.
    - No in-code SQL migration: the conversion is at the manager level
      (read old stores, write new form). Removal of the obsolete tables
      is a documented one-off (`DROP TABLE conclusions;`,
      `DROP TABLE conclusion_indexes;`) once step 14 lands.
    - Tests: a KB built with conclusion rules (e.g. via the sample KBs in
      `server/src/main/kotlin/io/rippledown/kb/sample/`) interprets every
      case identically before and after migration.
12. **Chat naming flow.** "Add the comment …" creates the comment
    attribute; the LLM proposes a semantic name with `C1`-style fallback;
    the confirmation message states the name and that it can be changed.
    A rename command updates `Attribute.name` only (id-referenced, so
    nothing else changes). Renaming refuses names in use (same rule as
    step 8c). Touch points: `AddComment`/`RemoveComment`/`ReplaceComment`
    in `server/.../kb/chat/action/`, the instruction files
    `3_defining_the_report_change.md` and `4_comment_variables.md`, and a
    new rename action + instruction.
13. **Diffs and interpretation views.** Rework `Interpretation`,
    `RuleSummary`, the diff types (`Addition`/`Removal`/`Replacement` in
    `common/.../model/diff/`) and `InterpretationViewManager`
    (`server/src/main/kotlin/io/rippledown/kb/InterpretationViewManager.kt`)
    to be backed by comment-attribute assignments rather than conclusions.
    Remove the conclusion ordering machinery — `conclusionOrderStore()` on
    `PersistentKB`, `PostgresConclusionOrderStore`, and the
    `OrderedEntityManager` base of `InterpretationViewManager` (resolved
    decision 4: ordering is not significant).
14. **Retire `GivenConclusion`.** Only after 10–13 are green: delete
    `GivenConclusion`, `Conclusion`, `ConclusionManager`,
    `ConclusionProvider`, the conclusion store, and `Rule.conclusion`
    (making `Rule.assignment` the single action field; `RuleTreeChange`
    loses its conclusion subclasses and `alignWith`). One rule action
    kind remains: `AssignValue`. Expect wide but mechanical fallout in
    tests; do not weaken assertions while converting them.

### Phase 3 — presentation and report

15. **Comments panel.** Show each comment in a two column table, rather than sequentially, with its attribute name and
    value
    (`ui/src/main/kotlin/io/rippledown/interpretation/`); comment
    attributes excluded from the case data table.
16. **Derived values panel.** New collapsible panel (UI label "Derived
    values" — not "Derived attributes"; see Presentation) under the case
    view (alongside Comments and Report) listing non-comment derived
    attributes as name/value pairs; hidden when empty; excluded from the
    case data table; also shown for cornerstones during rule building.
17. Hovering over a derived attribute name shows both its formula and the conditions that have assigned it a value,
    similar to showing the conditions for a comment.
18. The data source is the materialised case (`RuleTree.materialise`) —
    the server should expose derived name/value pairs on the viewable
    case rather than the UI recomputing anything. Once this exists,
    rewrite the REST-based derived-value cucumber assertions from step 9
    to use the panel (Compose UI test ids, following the existing panels'
    conventions).
19. **AI report.** The report generator receives named comment attributes
    (name + value pairs) as inputs; update
    `server/src/main/resources/report/report_system_prompt.md`
    accordingly.

### Phase 4 — later: external attribute renaming

18. **External-name alias mapping.** A persisted map from external names
    (as sent by the external system) to attribute ids, consulted at case
    ingestion (`KB.createRDRCase` path) before attribute lookup by name.
    New store on `PersistentKB` (in-memory + Postgres implementations,
    table `external_name_aliases(external_name TEXT PRIMARY KEY,
    attribute_id INT)`); the collision mangling from step 3 records its
    alias here (`A` → the `A (external)` attribute).
19. **Renaming external attributes.** With the alias map in place, the
    user can rename any external attribute (including mangled ones) via
    the chat; incoming cases still match through the alias, so data is
    never split across attributes. Name-in-use refusal applies as for
    derived attributes.

### Sequencing notes

- Steps 8a, 8b, 8c+8d, and the backdoor/assertion parts of step 9 are
  independent of each other and can be done in any order; the chat
  scenarios of step 9 need 8c+8d first.
- Phases 0–1 are independently shippable: they add repeat inferencing and
  derived data attributes without touching comment behaviour.
- Phase 2 is the large, breaking phase; steps 10–14 should land together
  behind passing migration tests before any release.
- Phase 4 is independent of Phases 2–3 and can land any time after
  Phase 0. Until it does, mangled names are stable and deterministic, so
  no data is at risk — the alias map can be backfilled from the mangling
  convention when it arrives.
- Episode history (resolved decision 5) needs nothing in any phase; the
  model leaves room for it because derived values are ordinary values in
  episodes.

## Testing

- Derived-attribute model tests: KB-owned flag, comment vs data subtype,
  collision mangling (deterministic, stable across cases, logged), naming/
  renaming refusal for names in use, reset/strip semantics, assignment to
  latest episode only.
- Value-expression tests: literal and formula evaluation, no assignment
  when a referenced attribute has no value, formulas referencing derived
  attributes (resolved across passes), formula overridden by a conditioned
  child rule, serialization and attribute alignment,
  `referencedAttributes()`.
- Comment-attribute tests: auto-naming and rename safety, `${}` variable
  placeholders in values, Comments panel presentation, report generation
  from named comment attributes.
- `RuleTree`/`KB` tests: multi-pass convergence, first-pass semantics of
  absence conditions, idempotent re-interpretation.
- Dependency-graph tests: edge construction (including inherited path
  conditions and value-expression references), `wouldCreateCycle`,
  suggester filtering of cycle-creating conditions, manual entry refused
  with a message naming the cycle, commit invariant.
- Rule-session tests: cornerstone behaviour with chained rules.
- Cucumber: end-to-end scenario — build a rule assigning
  `Diabetes status = "diabetic"`, then build a dependent rule conditioned
  on it, verify the dependent comment is given; a numeric-value scenario
  (`Risk score > 5`); a scenario using `is not in case`; scenarios showing
  a cycle-creating condition is neither suggested nor accepted when
  entered manually; a formula scenario — an unconditional
  rule assigning `BMI = weight / (height * height)`, a dependent rule
  conditioned on `BMI > 28`, and a case lacking `height` getting no BMI.
