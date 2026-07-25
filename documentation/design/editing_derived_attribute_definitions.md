# Editing the Definition of a Derived Attribute

## Status

**DRAFT — design in progress.** Not yet implemented. This document captures
the agreed design direction so far; open questions are listed at the end.
See also [repeat_inferencing.md](repeat_inferencing.md), on which this builds.

## The requirement

From `documentation/tickets/TODO.md` (Derived attributes):

> Allow the user to edit the formula or value of a derived attribute, as
> distinct from adding/removing/replacing it with a rule.

The user wants to correct the *definition* of a derived attribute (e.g. fix a
typo in the `BMI` formula) so the correction applies everywhere the attribute
is given by its definition — **without** building a new conditioned rule and
**without** a cornerstone review.

## Key realisation: comments already do this

Comments are the precedent to copy. A `Conclusion` is a **persisted,
first-class entity** (`server/.../persistence/ConclusionStore.kt`); rules
reference it **by id** (`PersistentRule.conclusionId` in
`server/.../persistence/RuleStore.kt`); and editing a comment's text is an
in-place update via `ConclusionStore.store(conclusion)`
(`server/.../persistence/postgres/PostgresConclusionStore.kt`). **No rule is
ever edited** — every rule referencing that conclusion id reflects the new
text immediately.

Non-comment derived attributes do **not** work this way today: the value
expression is embedded in the rule action, not stored on the attribute:

```kotlin
// common/.../model/rule/RuleAction.kt
@SerialName("AssignValue")
data class AssignValue(val attribute: Attribute, val expression: ValueExpression) : RuleAction()
```

That embedding is what would otherwise force an "edit the rule" approach,
which we are explicitly rejecting. **We do not edit rules.**

## Design direction (agreed)

> Rules should point to a derived attribute as their action, and derived
> attributes should be persisted (with their definition) like comments are.

### 1. Persist the definition on the derived attribute

Introduce a `DerivedDefinitionStore` keyed by attribute id, holding the
attribute's `ValueExpression` (its `Formula` or `Literal`), with a manager
mirroring `ConclusionManager`. Crucially it exposes an in-place
`store(attributeId, expression)` update — the editing primitive, exactly like
`ConclusionStore.store`.

Rationale for a dedicated store (rather than a column on the attributes
table): keep `Attribute` a pure identity (`id, name, kind`), and converge with
Phase 2 of repeat inferencing (comments-as-attributes), which needs the same
"attribute id → value expression (+ variables)" mapping. The two should become
one store.

### 2. The rule action references the attribute, not the formula

Introduce a `ByDefinition` `ValueExpression` whose evaluation delegates to the
attribute's stored definition:

- A normal "assign BMI" rule becomes `AssignValue(bmi, ByDefinition)` — the
  rule simply points at the attribute.
- `RuleTree.materialise` is given a resolver `(Attribute) -> ValueExpression?`;
  `ByDefinition.evaluate(case)` looks up the definition and evaluates it. This
  keeps `ValueExpression.evaluate` pure and testable.

`AssignValue.expression` is therefore either:

- `ByDefinition` — points at the attribute's stored definition (the normal
  case), or
- a concrete `Literal`/`Formula` — a deliberate per-rule **override** (the RDR
  exception; see below).

This is minimally disruptive: `PersistentRule.assignment` JSON stays valid and
leaf-most-wins semantics are unchanged.

### 3. Editing the definition

`DerivedDefinitionManager.store(attributeId, newExpression)` + re-interpret.
No rule session, no rule mutation, no cornerstone ceremony. Every
`ByDefinition` rule picks up the change automatically — the comment-editing
pattern applied to data attributes.

## Preserving RDR refinement

`repeat_inferencing.md` lists a real benefit: a formula can be overridden by a
conditioned child rule (e.g. a corrected `BMI` for amputees). We must keep it.
The two operations map cleanly onto the two representations:

- **Edit the definition** (this ticket) → update the attribute's stored
  `ValueExpression`. The *default*. Affects every `ByDefinition` rule. No new
  rule.
- **Replace with a rule** (existing `REPLACE_DERIVED_VALUE`) → a conditioned
  child rule carrying a concrete override expression, built through the normal
  rule session with cornerstone review. The RDR exception.

## Chat action

New action `EditDerivedAttributeDefinition(attributeName, valueExpression)`
with constant `EDIT_DERIVED_DEFINITION = "EditDerivedAttributeDefinition"` in
common chat `Constants.kt`. (Reflection contract in `ActionComment.kt`: the
JSON `action` string must equal the action class's simple name.)

Behaviour in `RuleService`/`RuleSessionManager`:

- Resolve the attribute (`attributeForName`); require `kind == DERIVED`.
- Parse `valueExpression` via the existing `valueExpressionFor`
  (`RuleSessionManager.kt`) — literal-vs-formula rules and auto-creation of
  referenced attributes come for free.
- Cycle guard: reuse `checkActionExpressionIsAcyclic`, but reading references
  from the attribute definition (see supporting changes).
- `definitionManager.store(attr.id, expression)`; re-interpret; push updated
  status.
- Return an old→new summary, e.g. *"Changed the definition of BMI from
  `weight / height ^ 2` to `weight / (height * height)`."*

It does not open a rule session and is rejected if one is active (matching the
guards in `AssignDerivedValue.kt`).

## Distinguishing "edit" from "replace with a rule" (the crux)

Instruction routing (new `18_editing_derived_definition.md`, sibling to
`17_assigning_derived_values.md`):

- **Edit** → a global correction with no condition/reason: *"Fix the BMI
  formula to weight/(height\*height)"*, *"BMI should be defined as …"*, *"edit
  the definition of Risk score"* → `EditDerivedAttributeDefinition`.
- **Replace with a rule** → case- or condition-specific: *"For amputees, BMI
  should be …"*, *"when Height is missing, …"* → `ReplaceDerivedValue` (rule
  session).
- Ambiguous → ask a one-line clarifying question ("Change the definition
  everywhere, or only under a condition?").

## Required supporting changes

- **Dependency graph / cycle detection.**
  `DerivedAttributeDependencyGraph` currently derives value-expression edges
  from rule actions (`expressionReferences()` on `RuleTreeChange`). With the
  formula on the attribute, edges for `ByDefinition` rules must be read from
  the attribute's stored definition. Editing a definition must run
  `cycleCreatedBy` against that graph before committing.
- **Panel/tooltip.** `ViewableCase.derivedValues()` reads
  `assignment.expression.asText()`; for `ByDefinition` show the attribute's
  definition text, for overrides the override text.
- **Persistence/migration.** New table
  `derived_definitions(attribute_id INT PRIMARY KEY, expression TEXT)`;
  `SchemaUtils.create` covers fresh DBs; state the one-off `ALTER`/backfill in
  the PR per the repeat_inferencing migration convention. Backfill: for
  existing `AssignValue` rules, move the embedded expression onto the
  attribute definition and rewrite the rule action to `ByDefinition` where the
  attribute has a single consistent expression; genuinely differing
  expressions remain as overrides.

## Relationship to Phase 2 (comments become derived attributes)

This is the same generalisation Phase 2 makes for comments. The
`DerivedDefinitionStore` should be shaped so a `COMMENT` attribute's text
template (+ `${}` variables) is just another definition, so Phase 2 can fold
`ConclusionStore` into it rather than maintaining two parallel stores. This
directly realises "persist derived attributes like we do comments".

## Testing

- **Store/manager**: `store` updates the definition; a second edit overwrites;
  rules are untouched (ids unchanged).
- **Inference**: `ByDefinition` resolves to the stored expression; a
  conditioned override still wins (leaf-most); editing the default does not
  disturb overrides.
- **Cycle guard**: editing a definition to reference itself
  (directly/indirectly) is refused with the cycle message.
- **Chat action**: mocked `RuleService` — edit routed; session-active
  rejection; not-derived rejection.
- **Cucumber**: build `BMI = weight/(height*height)` as a definition, edit it,
  assert both an existing and a new case get the corrected value; a contrasting
  scenario where "for amputees …" still creates an override rule (cornerstone
  review); a routing scenario that "fix/edit the definition" does not open a
  rule session.

## Open questions (for tomorrow)

1. **Store shape now vs Phase-2 unification.** Build `DerivedDefinitionStore`
   for data attributes only now, or design the unified attribute-value store
   (covering comment text + variables) up front?
2. **`ByDefinition` vs no-expression action.** Is a `ByDefinition`
   `ValueExpression` the cleanest representation, or should the rule action
   reference the attribute with no expression at all and always resolve via the
   definition store?
3. **Migration/backfill of existing KBs.** Confirm the backfill heuristic
   (single consistent expression → definition; differing → overrides) and
   whether any configured test KBs contain `AssignValue` rules that need it.
4. **Safety on a global edit.** Editing a definition changes values KB-wide.
   Do we want a lightweight impact summary (e.g. count of affected
   cornerstones) or a confirmation step, given there is deliberately no
   cornerstone session?
5. **Naming vs value edit.** This ticket is the formula/value definition only;
   renaming a derived attribute is a separate TODO item.

## Where we got to (session note)

- Agreed to **abandon** the earlier "edit the rule action in place" sketch.
- Agreed on the **definition-on-the-attribute** model above, mirroring the
  conclusion store, with a `ByDefinition` rule action and per-rule overrides
  retained for RDR refinement.
- Next session: resolve the open questions (start with 1 and 2), then flesh out
  the `DerivedDefinitionStore` interface and the `ByDefinition` type as the
  first concrete code step (TDD, per repeat_inferencing conventions).
