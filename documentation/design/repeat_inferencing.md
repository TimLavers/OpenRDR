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

A derived attributes is added to the case (or removed or replaced) by a rule, in the same was as a comment is given.

Once a derived attribute has been assigned a value and added to the case, it
is indistinguishable from an externally assigned attribute for the purposes of condition evaluation, with one
difference: the knowledge base only assigns values in the most recent episode of the case. (This may change in the
future, i.e. we may want the KB to assign values to previous episodes.)

Some examples:

- `Diabetes status = "diabetic"`
- `Risk score = 7`
- `BMI = weight / height ** 2`

Both the attribute (user-named at rule-building time) and the value
expression (a literal or a formula)are chosen by the user.

### Value expressions

There are, in effect, two kinds of derived attribute:

- Concepts like `Diabetes status`: conditions plus a *literal* value;
- Concepts like `BMI` or `creatinine clearance ratio`: conditions (or just the condition `true`) plus a *computed* (
  formula-based) value.

The rules are probably more useful for the former concepts, whereas the formula-based concepts will generally just be
given for every case.

These are unified rather than modelled as separate types: the assigned
value is a *value expression*, of which a literal is the trivial case.

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
2. there is a rule that removes it

The expression language is deliberately small at first: arithmetic on the
latest values of attributes (`+ - * / **`, parentheses, numeric literals).
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
  KB-owned/external distinction structural rather than a matter of styling. The panel is always visible, even when the
  case has no derived attributes, so the user is made aware of this facility.
  It is labelled **"Derived attributes"**.
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
   a derived quantity such as BMI), and the model leaves room for them as derived values are ordinary values in
   episodes. Not for the first implementation however. When we do this, we will need to consider the rules associated
   with the derived attribute, and how they should be applied to previous episodes, e.g. by successively stripping the
   most recent episode and then re-interpreting the case.
7. **Conclusions as derived attributes**: Comments-as-conclusions run deep
   (`Conclusion`, `RuleSummary`, interpretation diffs, the conclusion
   store, conclusion ordering). Existing KBs need each conclusion
   converted to a comment attribute plus assignment.
8. **Attribute creation UX**: The user adds a non-comment derived attribute by naming it and typing it as text or
   numeric. Within the scope of this rule session, they cannot add another derived attribute.

## Alternative considered and rejected: pre-processing evaluation

Before committing to Phase 2, an alternative to rule-driven derived attributes was considered: evaluate every
derived-attribute definition once as a *pre-processing step* before the rules run, adding the attribute to the case
whenever its value is not blank/null. No rules would be needed to add a derived attribute; concepts genuinely needing
rules (e.g. `Diabetes status`)
would instead be expressed as "abbreviated comments" and used as conditions via repeat inferencing on comments.

The attraction is reduced cognitive load: the user would not have to "add"
formula attributes like `BMI` to the case with an (unconditional) rule.

Rejected, for these reasons:

1. **The cognitive load it saves is already near zero.** Requesting a derived attribute with no reason creates an
   unconditional root rule, so from the user's perspective it already *is* "evaluated for every case" — no extra
   conceptual step is visible in the chat flow.
2. **Refinement would be lost.** A pre-processing step evaluates each definition once, uniformly. The rule-driven model
   gives conditioned overrides (e.g. a corrected `BMI` for amputees), conditional removal and replacement — exactly the
   RDR machinery. Exceptions bolted onto a pre-processing step would reinvent rules.
3. **Abbreviated comments are a weaker substitute.** Comments are text; derived attributes carry numeric values, so
   `Risk score > 5`, trends, and formulas composing other derived attributes all work. A
   `"diabetic status"` comment cannot support these.
4. **The intermediate/final distinction becomes the user's problem.** With derived attributes shown in their own panel
   and excluded from report inputs, "intermediary, not report content" is *structural*. Folding intermediaries into
   comments would force a naming convention (e.g. a
   `#` prefix) to keep them out of the AI report — a worse cognitive load than the one removed, and fragile. Nor can
   "used as a condition in another rule" be inferred to mean "not for the report": many true intermediate comments may
   be given for a case yet be irrelevant to the final report (e.g. detailed GP-directed comments all suppressed when the
   referrer is a specialist).

Conclusion: keep the current plan. Derived attributes remain rule-driven and user-visibly distinct from comments (the
report is based on comments only), while Phase 2 still unifies the machinery underneath.

## Out of scope

- Grouping derived attributes into folders.
- Assigning the value of a derived attribute using an AI rather than with a rule ("please read the clinical notes and
  assign diabetic status to be "diabetes" if indicated")
- Referring to derived attributes from *other* knowledge bases.
- Historical derived values in earlier episodes (see resolved decision).
- Conditions on report structure (e.g. that would set the ordering or filtering of report sections).

## Implementation plan

The plan is phased: derived data attributes and repeat inferencing first, with
comments untouched; comments are recast as derived attributes in a second
phase. Each step follows TDD: tests are written before the production code they cover, and each step leaves all tests
green, including cucumber tests.

### Status (as of this revision)

**Done:** all of Phase 0 (steps 1–3) and all of Phase 1 (steps 4–11) —
including the "Derived attributes" UI panel and its tooltip.

**Remaining:** none of the
subsequent phases have been started — Phase 2 (comments become derived
attributes; steps 12–16, `Conclusion` / `AssignConclusion` still exist),
Phase 3 (presentation and report; steps 17–19), and Phase 4 (external
attribute renaming; steps 20–21). The completed steps below have been
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

- `RuleAction` / `AssignConclusion` / `AssignValue(attribute, expression)`
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

### Phase 1 step 8 — suggestions and chat (DONE)

Implemented as four sub-steps; key symbols, all covered by tests:

- **8a. Session-case materialisation.** `RuleSessionManager.conditionHintsForCase`
  and `conditionForExpression` build/evaluate against
  `kb.ruleTree.materialise(case)`, so derived values assigned by existing
  rules are visible to suggestions and typed conditions. `ConditionSuggester`
  stays a pure function of its context.
- **8b. Suggestions for KB derived attributes.**
  `ConditionSuggester.derivedAttributeSuggestions()` offers presence/absence
  conditions on `DERIVED` attributes not on the session case (only those
  that hold), included in `allSuggestions()` before `pruneCycleCreating` so
  cycle-creating ones are filtered.
- **8c. Chat actions.** Constants `ASSIGN_DERIVED_VALUE` /
  `REMOVE_DERIVED_VALUE` / `REPLACE_DERIVED_VALUE` in common chat
  `Constants.kt`; action classes `AssignDerivedValue` / `RemoveDerivedValue`
  / `ReplaceDerivedValue` in `server/.../kb/chat/action/`; `RuleService`
  gains `startRuleSessionToAssignValue` / `…RemoveAssignment` /
  `…ReplaceAssignment` taking a `ViewableCase`. External-name-in-use and
  expression-cycle errors are relayed to the chat verbatim.
- **8d. Chat instructions.** `17_assigning_derived_values.md` added
  (quoting rule for literal vs formula; JSON action shape);
  `16_listing_capabilities.md` lists assigning values to derived attributes.

### Phase 1 step 9 — cucumber (DONE)

`cucumber/src/test/resources/requirements/inferencing/Repeat inferencing.feature`
and `Derived attribute.feature` are implemented and no longer `@ignore`d.
Backdoor plumbing: `BuildRuleRequest` gained nullable `assignAttribute` /
`assignExpression`; `RuleSessionManager.buildRule` starts an assignment
session when `assignAttribute != null`; `RESTClient.buildAssignmentRule`
and the `BackdoorRuleStepDefs` "assign the value/formula … to the derived
attribute …" steps drive it (literal values quoted, formulas passed
as-is). Derived-value assertions read the materialised values via the
viewable case (`DerivedValueStepDefs`).

### Phase 1 steps 10–11 — derived-attributes UI (DONE)

These UI steps do not depend on comments becoming derived attributes, so
they were implemented as part of Phase 1 rather than deferred to Phase 3.

10. **Derived attributes panel.** Collapsible panel (UI label "Derived
    attributes"; see Presentation) under the case view — alongside Comments
    and Report — listing non-comment derived attributes as name/value pairs; always showing; excluded from the case data
    table; also shown for
    cornerstones during rule building. The data source is the materialised
    case via `ViewableCase.derivedValues()` (`RuleTree.materialise`), so the
    UI recomputes nothing. `ui/.../interpretation/DerivedValuesPanel.kt`,
    wired into `CaseInspection` and `CornerstoneInspection`.
11. **Derived-value tooltip.** Hovering over a derived attribute name shows
    both its formula and the conditions that have assigned it a value,
    similar to showing the conditions for a comment.

### Phase 2 — comments become derived attributes

This phase resolves the temporary duplication that Phase 1 introduces
between `AssignConclusion` and `AssignValue` (parallel rule actions,
tree-change and changer classes). A `Conclusion` is really just an
`Attribute` (of `COMMENT` kind) plus a `ValueExpression` (a text template
whose `${}` variables are attribute references), so once comments are
derived attributes, `AssignConclusion` and its parallel machinery are
deleted. The one semantic difference to design for: a report is a *set*
of comments from independent rules, whereas a derived attribute holds a
single value (leaf-most rule wins) — hence one comment attribute per
conclusion rather than one shared "report" attribute.

Phase 2 is large and breaking: steps 12–16 land together behind passing
migration tests before any release. Before starting, re-read this whole
document and survey the current usages of `Conclusion` (it reaches into
`Interpretation`, `RuleSummary`, the diff types, `ConclusionManager`,
`InterpretationViewManager`, the conclusion store and order store, the
chat comment actions, and the UI interpretation package). Sequence the
work bottom-up as below, keeping the build green between sub-steps by
leaving `AssignConclusion` in place until step 16.

12. **Comment attributes.** Each `Conclusion` is replaced by a `COMMENT`
    attribute whose assigned value is the comment text.
    - `AttributeManager` (`server/src/main/kotlin/io/rippledown/kb/AttributeManager.kt`)
      gains comment-attribute support: creation with auto-naming (`C1`,
      `C2`, … — smallest unused index), lookup of all `COMMENT`
      attributes.
    - `${}` comment-variable support moves to the assigned value: the
      rendering currently done via `Conclusion`/`Interpretation.toComments`
      is re-implemented as a dedicated `ValueExpression` subtype,
      `CommentTemplate(text, variables)` — id-based attribute references (rename-safe, like `AttributeValue`), rendering
      semantics preserved exactly. *(Implemented 2 Aug 2026.)* The tests in the comments cucumber features
      (`cucumber/src/test/resources/requirements/comments/`) pin the semantics.
13. **Migration of configured KBs.** *(Resolved 2 Aug 2026: a one-off conversion, not load-time migration logic.)* The
    only KBs in the old format are those in this project — the zip files under
    `server/src/test/resources` and the zoo KB under
    `server/src/main/resources`; there are no external databases to convert. So the conversion is a well-tested one-off
    applied to those fixtures before step 16 lands, after which the migrator is deleted rather than carried in the KB
    load path forever.
    - Each conclusion is converted to a `COMMENT` attribute plus assignment:
        - for each `Conclusion` in the conclusion store, create a
          `COMMENT` attribute (auto-named) whose stored definition is a
          `CommentTemplate` of the conclusion text and variables;
        - each rule whose `conclusionId` references it becomes a rule with `AssignValue(attribute, ByDefinition)`
          (update
          `PersistentRule`: `conclusionId` becomes unused);
        - one-way and idempotent: migrated KBs have an empty conclusion store, so re-running is a no-op; exports after
          migration use the new form only.
    - No in-code SQL migration: the conversion is at the store level (read old stores, write new form) via
      `RuleStore.update` and
      `ConclusionStore.clear`. Removal of the obsolete tables is a documented one-off (`DROP TABLE conclusions;`,
      `DROP TABLE conclusion_variables;`,
      `DROP TABLE conclusion_indexes;`) once step 16 lands.
    - After step 16, KB load/import `check`s that the conclusion store is empty — one cheap guard against any stray old
      export, in place of a permanent migration subsystem.
    - Tests: a KB built with conclusion rules (e.g. via the sample KBs in
      `server/src/main/kotlin/io/rippledown/kb/sample/`) interprets every case identically before and after migration,
      comparing rendered comment texts (including unresolved-variable markers).

14. **Chat naming flow.** *(Implemented 10 Aug 2026.)* "Add the comment …"
    creates the comment attribute; the LLM proposes a semantic name with
    `C1`-style fallback; the user is told the name when the comment is accepted, and that it can be changed. A rename
    command updates
    `Attribute.name` only (id-referenced, so nothing else changes). Renaming refuses names in use (same rule as step
    8c).
    - The model carries an `attributeName` on `{{ADD_COMMENT}}` and
      `{{REPLACE_COMMENT}}`. It is only a proposal: `AttributeManager`
      `createCommentAttribute(proposedName)` falls back to `C1`, `C2`, … if it is blank, in use, or longer than
      `MAX_PROPOSED_ATTRIBUTE_NAME_LENGTH` (20 — names are labels, so a long one means the model did not comply). An
      existing comment attribute, reused because the text already has one, keeps its name.
    - *(Resolved: announce at acceptance, not at commit.)* The name is stated deterministically by the server
      (`ChatResponse.withCommentName`, prefixed to the response for `AddComment`/`ReplaceComment`), not left to the
      model, and the session's attribute is available through
      `RuleService.nameOfCommentAttributeInSession()`.
    - *(Resolved: renaming covers derived attributes too, not just comments.)* `RENAME_ATTRIBUTE` → `RenameAttribute`
      action →
      `RuleService.renameAttribute`, refused for `EXTERNAL` attributes (until the alias map of step 20 exists). Renaming
      is not rule building, so it is allowed whether or not a session is in progress; the logic lives in
      `AttributeManager.rename` and the `RuleService`
      method is a delegation. (`RuleService` is the chat's façade onto the KB and already carries non-session operations
      such as
      `moveAttributeTo` and `editDerivedAttributeDefinition`; splitting an
      `AttributeService` out of it is a separate refactor.)
    - Renaming is in place: `Attribute.name` is a `var`, so every holder of the attribute sees the new name. Holders
      deserialized separately would otherwise carry a stale name, so they are aligned with the attribute manager when
      the KB is loaded: `AssignValue.alignAttributes`
      in `RuleManager`, and `ValueExpression.alignAttributes` in
      `DerivedDefinitionManager`.
    - The client had to be told that a rename is a change: an `Attribute` is equal to another with the same id whatever
      its name, so a refreshed case whose only change is a renamed attribute is structurally equal to the case it
      replaces, and the default `mutableStateOf` policy discarded it, leaving the old name on screen. `OpenRDRUI` holds
      the current case with
      `neverEqualPolicy()`.
    - Cukes: `cucumber/.../requirements/chat/Naming and renaming.feature`. The messages are matched through the
      `COMMENT_IS_NAMED`, `CAN_BE_RENAMED`,
      `RENAMED` and `CANNOT_BE_RENAMED` constants, because the name itself is chosen by the model.
      `gradlew :cucumber:cucumberDryRun` checks every step of every feature is defined, without a server or the LLM.
    - Instructions: new `19_naming_and_renaming.md`, referenced from
      `3_defining_the_report_change.md`; `RENAME_ATTRIBUTE` added to
      `13_json_format_guidelines.md` and `16_listing_capabilities.md`.
15. **Diffs and interpretation views.** Rework `Interpretation`,
    `RuleSummary`, the diff types (`Addition`/`Removal`/`Replacement` in
    `common/.../model/diff/`) and `InterpretationViewManager`
    (`server/src/main/kotlin/io/rippledown/kb/InterpretationViewManager.kt`)
    to be backed by comment-attribute assignments rather than conclusions.
    Remove the conclusion ordering machinery — `conclusionOrderStore()` on
    `PersistentKB`, `PostgresConclusionOrderStore`, and the
    `OrderedEntityManager` base of `InterpretationViewManager` (resolved decision 4: ordering is not significant). *(
    Done: comments are shown in id order — conclusions, which only a KB not yet converted has, ahead of assignments.
    `OrderedEntityManager` remains, as attribute ordering in the case view is significant.
    `DROP TABLE conclusion_indexes;` joins the documented one-offs of step 13.)* *(Resolved 2 Aug 2026: one comment
    attribute per comment text.)*
    "Replace this comment with X" mints a new comment attribute for X:
    the replacing rule assigns the new attribute, and leaf-most suppression retracts the parent's — one rule, no
    explicit retraction. This preserves the invariant that a comment attribute's definition is its text, so a future
    "change this comment's wording everywhere" is exactly the definition-edit flow used for derived formulas. A
    conditional per-case wording override of the *same*
    attribute was considered and rejected: it would make "the comment's text" ambiguous.
16. **Retire `AssignConclusion`.** Only after 12–15 are green: delete
    `AssignConclusion`, `Conclusion`, `ConclusionManager`,
    `ConclusionProvider`, the conclusion store, and `Rule.conclusion`
    (making `Rule.assignment` the single action field; `RuleTreeChange`
    loses its conclusion subclasses and `alignWith`). One rule action
    kind remains: `AssignValue`. Expect wide but mechanical fallout in
    tests; do not weaken assertions while converting them.

### Phase 3 — presentation and report

17. **Comments panel.** Show each comment in a two column table, rather than sequentially, with its attribute name and
    value
    (`ui/src/main/kotlin/io/rippledown/interpretation/`); comment
    attributes excluded from the case data table.
18. **Cucumber assertions via the panel.** The Derived attributes panel
    (Phase 1 step 10) and its viewable-case data source
    (`ViewableCase.derivedValues()`) already exist. Rewrite the remaining
    REST-based derived-value cucumber assertions (currently in
    `Repeat inferencing.feature`) to read from the panel instead (Compose
    UI test ids, following the existing panels' conventions).
19. **AI report.** The report generator receives named comment attributes
    (name + value pairs) as inputs; update
    `server/src/main/resources/report/report_system_prompt.md`
    accordingly.

### Phase 4 — later: external attribute renaming

20. **External-name alias mapping.** A persisted map from external names
    (as sent by the external system) to attribute ids, consulted at case
    ingestion (`KB.createRDRCase` path) before attribute lookup by name.
    New store on `PersistentKB` (in-memory + Postgres implementations,
    table `external_name_aliases(external_name TEXT PRIMARY KEY,
    attribute_id INT)`); the collision mangling from step 3 records its
    alias here (`A` → the `A (external)` attribute).
21. **Renaming external attributes.** With the alias map in place, the
    user can rename any external attribute (including mangled ones) via
    the chat; incoming cases still match through the alias, so data is
    never split across attributes. Name-in-use refusal applies as for
    derived attributes.

### Sequencing notes

- Phases 0–1 are independently shippable: they add repeat inferencing and
  derived data attributes without touching comment behaviour.
- **Editing derived-attribute
  definitions ([editing_derived_attribute_definitions.md](editing_derived_attribute_definitions.md))
  lands before Phase 2.** It introduces the `DerivedDefinitionStore` /
  `ByDefinition` architecture that Phase 2 should land on: a comment then becomes a `COMMENT` attribute whose definition
  is a text template, and
  `ConclusionStore` folds into the definition store. Doing Phase 2 first would migrate conclusions into the embedded-
  `AssignValue` form only to re-migrate them when the definition store arrives — two breaking migrations of configured
  KBs instead of one. It is also the smaller, non-breaking piece, so landing it first de-risks Phase 2.
- Phase 2 is the large, breaking phase; steps 12–16 should land together
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
