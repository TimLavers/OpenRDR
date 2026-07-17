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
absent. This keeps `is in case` conditions on formula outputs meaningful
downstream, and means formulas need no guard conditions.

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

The existing comment action trichotomy maps directly onto assignment:

- *add comment* → assign a value to a new comment attribute
- *remove comment* → refinement rule retracting the assignment
- *replace comment* → child rule assigning a different value to the same
  attribute

Conflicts between rules assigning the same attribute are resolved by the
existing RDR refinement structure: the leaf-most satisfied rule for an
attribute wins, exactly as `Rule.apply` works today for conclusions.

Comment values must continue to support the existing `${}` attribute
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

A final check at commit remains as an internal invariant (defence in
depth), but with the entry points guarded it should never fire.

The checks live in the rule session machinery on the server; the graph is
computed from the rule tree on demand (no persistence needed).

## Derived attributes are KB-owned

- Derived attributes are flagged as KB-assigned, distinguishing them from
  external attributes.
- The external system cannot supply values for them. If an incoming case
  has an external attribute whose name matches a derived attribute, the
  external attribute's name is mangled deterministically (e.g. `A` →
  `A (external)`) — case processing must never fail and clinical data must
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
5. **Episodic derived attributes**: deferred. Scenarios where derived
   values in earlier episodes would be useful are imaginable (e.g.
   conditions like `previous Diabetes status is "diabetic"`, or trends in
   a derived quantity such as BMI), and the model leaves room for them —
   derived values are ordinary values in episodes. But this would require
   persisting past interpretations, and would also affect the Derived
   attributes panel, which assumes a single value per attribute.
   Definitely not for the first implementation.

## Open questions (to be decided)

1. **Migration and phasing.** Comments-as-conclusions run deep
   (`Conclusion`, `RuleSummary`, interpretation diffs, the conclusion
   store, conclusion ordering). Existing KBs need each conclusion
   converted to a comment attribute plus assignment. Options: introduce
   derived data attributes and repeat inferencing first, recast comments
   in a second phase; or do the full remodel at once.
2. **Attribute creation UX.** How does the user introduce a new
   non-comment derived attribute during rule building (naming, typing as
   text vs numeric)?

## Out of scope

- Referring to derived attributes from *other* knowledge bases.
- Historical derived values in earlier episodes (see resolved decision 5).
- Conditions on report structure (ordering, sections).

## Implementation plan

The plan is phased, resolving open question 1 in favour of the lower-risk
option: derived data attributes and repeat inferencing land first, with
comments untouched; comments are recast as derived attributes in a second
phase. Each step follows TDD: tests are written before the production code
they cover, and each step leaves all tests green.

### Phase 0 — groundwork: attribute kinds and case support

1. **Attribute kind.** Add `kind: AttributeKind = EXTERNAL` to `Attribute`
   (`common/src/main/kotlin/io/rippledown/model/Attribute.kt`), with
   `AttributeKind` enum `EXTERNAL`, `DERIVED`, `COMMENT`. Update
   serialization (backwards-compatible default `EXTERNAL`),
   `AttributeManager` and the attribute store
   (`server/src/main/kotlin/io/rippledown/kb/AttributeManager.kt`
   and its persistence), and KB export/import so the kind round-trips.
   Existing KBs load unchanged (all attributes default to `EXTERNAL`).
2. **Case support for derived values.** On `RDRCase`
   (`common/src/main/kotlin/io/rippledown/model/RDRCase.kt`). Since case
   data is immutable (the episode structures are computed at
   construction), these return copies, which the Phase 1 fixpoint loop
   threads through its passes; both carry over the case id and
   interpretation:
    - `withDerivedValue(attribute, value)` — a copy with the value
      assigned in the latest episode;
    - `withoutDerivedValues()` — a copy with all values of non-`EXTERNAL`
      attributes removed.
3. **External collision guard.** In `KB.processCase`/`createRDRCase`
   (`server/src/main/kotlin/io/rippledown/kb/KB.kt`), an externally
   supplied attribute whose name matches a derived attribute is mapped to
   a deterministically mangled external attribute (`A` → `A (external)`),
   created once and reused; the collision is logged. Derived-attribute
   naming/renaming refuses names already in use.

### Phase 1 — rule actions and repeat inferencing

4. **RuleAction abstraction.** Introduce a `RuleAction` type on `Rule`
   (`common/src/main/kotlin/io/rippledown/model/rule/Rule.kt`) with two
   implementations for now:
    - `GiveConclusion(conclusion)` — wraps the existing conclusion
      behaviour, so current rules and their persistence are unchanged in
      meaning (the existing `conclusion` field backs it);
    - `AssignValue(attribute, expression)` — the new assignment action,
      where the expression is a literal or an arithmetic formula over the
      latest values of other attributes; if any referenced attribute has no
      value in the case, no assignment is made.
      `Rule.apply` applies the leaf-most satisfied rule's action; for
      `AssignValue` this contributes an assignment instead of a conclusion.
      A small `ValueExpression` type accompanies this: `Literal(value)` and
      `Formula(arithmetic over attribute references)`, with evaluation
      against a case, serialization, `alignAttributes`, and a
      `referencedAttributes()` accessor for the dependency graph.
5. **Fixpoint inference.** Rework `RuleTree.apply`
   (`server/src/main/kotlin/io/rippledown/model/rule/RuleTree.kt`):
   strip derived values, then loop — evaluate, collect assignments, write
   them to the latest episode — until assignments and conclusions are
   unchanged from the previous pass. No iteration cap: termination is
   guaranteed by acyclicity (step 7). All entry points already funnel
   through `KB.interpret`, including cornerstone evaluation in
   `RuleBuildingSession`, so no further call-site changes.
6. **Rule tree changes for assignment.** New `RuleTreeChange` subclasses in
   `server/src/main/kotlin/io/rippledown/model/rule/RuleTreeChange.kt` /
   `RuleTreeChanger.kt`: assign a value, retract an assignment, replace an
   assigned value — mirroring the add/remove/replace conclusion changers.
   `RuleSessionManager` gains the corresponding
   `startRuleSessionToAssignValue` (etc.) entry points.
7. **Dependency graph and cycle prevention.** New server-side class (e.g.
   `DerivedAttributeDependencyGraph`) built on demand from the rule tree:
   nodes are derived attributes; edges as defined above; a
   `wouldCreateCycle(condition, action)` query. Applied at three points:
    - `ConditionSuggester`: cycle-creating candidates filtered out before
      ranking, so they are never offered;
    - `RuleSessionManager.addConditionToCurrentRuleSession` and
      `conditionForExpression`: a manually entered cycle-creating condition
      is not added to the rule, and the response carries a message naming
      the cycle for the chat to relay;
    - `commitCurrentRuleSession`: internal invariant check (defence in
      depth); should never fire.
8. **Suggestions and chat.** Extend `ConditionSuggester`/`SuggestionContext`
   to offer conditions on derived attributes (current-case first, other KB
   derived attributes lower-ranked). Add chat instructions for the
   assign-value action, including attribute naming at rule-building time
   (open question 2 is decided here: the chat flow asks for or proposes a
   name; values are typed by example — numeric if the value parses as a
   number).
9. **Phase 1 cucumber.** End-to-end scenarios: assignment rule then
   dependent rule; numeric condition on an assigned value
   (`Risk score > 5`); `is not in case`; cycle prevention (not suggested;
   manual entry refused with explanation).

### Phase 2 — comments become derived attributes

10. **Comment attributes.** Each `Conclusion` is replaced by a `COMMENT`
    attribute whose assigned value is the comment text.
    `ConclusionManager` is superseded by comment-attribute handling in
    `AttributeManager`; `${}` variable support moves to the assigned
    value (rendering as in `Conclusion.render`).
11. **KB migration.** On KB load/import, each existing conclusion is
    converted to a comment attribute (auto-named `C1`, `C2`, …) and each
    rule's `GiveConclusion` action to an `AssignValue` on that attribute.
    One-way, idempotent migration; exports after migration use the new
    form only.
12. **Chat naming flow.** "Add the comment ..." creates the comment
    attribute; the LLM proposes a semantic name with `C1`-style fallback;
    the confirmation message states the name and that it can be changed.
    A rename command updates `Attribute.name` only (id-referenced, so
    nothing else changes).
13. **Diffs and interpretation views.** Rework `Interpretation`,
    `RuleSummary`, the diff types (`Addition`/`Removal`/`Replacement`) and
    `InterpretationViewManager` to be backed by comment-attribute
    assignments rather than conclusions. Remove
    `conclusionOrderStore`/ordering machinery (resolved decision 4).
14. **Retire `GiveConclusion`.** Once migration and views are done, delete
    the conclusion-backed action, `Conclusion`, and `ConclusionManager`.
    One rule action kind remains: `AssignValue`.

### Phase 3 — presentation and report

15. **Comments panel.** Show each comment with its attribute name
    (`ui/src/main/kotlin/io/rippledown/interpretation/`); comment
    attributes excluded from the case data table.
16. **Derived values panel.** New collapsible panel (UI label "Derived
    values") under the case view (alongside Comments and Report) listing
    non-comment derived attributes as name/value pairs; hidden when empty;
    excluded from the case data table; also shown for cornerstones during
    rule building.
17. **AI report.** The report generator receives named comment attributes
    (name + value pairs) as inputs; update
    `server/src/main/resources/report/report_system_prompt.md`
    accordingly.

### Phase 4 — later: external attribute renaming

18. **External-name alias mapping.** A persisted map from external names
    (as sent by the external system) to attributes, consulted at case
    ingestion before attribute lookup by name. Collision mangling (step 3)
    records its alias here (`A` → the `A (external)` attribute).
19. **Renaming external attributes.** With the alias map in place, the
    user can rename any external attribute (including mangled ones) via
    the chat; incoming cases still match through the alias, so data is
    never split across attributes. Name-in-use refusal applies as for
    derived attributes.

### Sequencing notes

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
