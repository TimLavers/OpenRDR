# Derived attribute definitions

Requirement: a user can correct a derived attribute's formula or value everywhere it is given, without building a rule
and without a cornerstone review ([derived_attributes.md](../requirements/derived_attributes.md)).

## The definition lives on the attribute, not in the rule

Rules are never edited. If the formula were embedded in each rule's action, correcting a typo in `BMI` would mean
editing rules, which RDR forbids, or building a rule per case, which is not what "the formula is wrong" means.

So a derived attribute's expression is stored against the attribute id in a `DerivedDefinitionStore`, managed by
`DerivedDefinitionManager`, and the normal assignment rule carries the `ByDefinition` sentinel: "assign this attribute
its definition". Every such rule sees an edit immediately, exactly as every rule giving a comment sees that comment's
text. `Attribute` itself stays a pure identity `(id, name, kind)`.

The same store holds comment templates: a `COMMENT` attribute's `CommentTemplate` is just another definition. One
store, one shape (`attributeId → ValueExpression`, persisted as JSON so that a new expression type needs no schema
change).

## Two operations, two representations

| The user wants                                            | What happens                                                                                                                                                   |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| "Fix the BMI formula" — a global correction               | `DerivedDefinitionManager.store(id, expression)`, re-interpret. No rule, no cornerstones.                                                                      |
| "For amputees, BMI should be …" — a conditioned exception | A replacement rule whose action carries a concrete `Formula`/`Literal` **override**, built through the normal session with cornerstone review. Leaf-most wins. |

An override is the RDR refinement; editing the default does not disturb it. The chat instructions route on whether the
request carries a condition; an ambiguous request gets a one-line question ("Everywhere, or only under a condition?").

## Resolve, then evaluate

`ValueExpression.evaluate` is never called on `ByDefinition`. `RuleTree.materialise` is given a resolver
`(Attribute) -> ValueExpression?` and substitutes the definition before evaluating, so evaluation stays pure and
testable. The dependency graph and the panels use the same resolver, so the tooltip for a `ByDefinition` rule shows the
definition text and for an override shows the override.

A definition edit is checked for cycles like any other expression, and is refused while a rule session is active.

## Not built

An impact summary when a definition is edited (how many cornerstones' values change); a cancelled assignment session
leaves its stored definition, harmless because the next attempt overwrites it.
