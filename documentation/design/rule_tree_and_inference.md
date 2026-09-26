# Rule tree and inference

Requirements: [derived_attributes.md](../requirements/derived_attributes.md), [comments.md](../requirements/comments.md).

## One rule action: assign a value to an attribute

A case arrives with **external** attributes, valued by the sending system. The knowledge base's work is modelled as
*adding attributes to the case*: a rule's action is `AssignValue(attribute, expression)`, where the attribute is one the
KB owns (`DERIVED` or `COMMENT`) and the expression is a `ValueExpression`. An interpretation is the set of assignments
made by the rules that fired.

| `ValueExpression` | Meaning                                                                                                                     |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `Literal`         | a fixed value, e.g. `"diabetic"`                                                                                            |
| `Formula`         | arithmetic over the latest values of attributes, e.g. `weight / height ^ 2`; no assignment if an input is missing           |
| `CommentTemplate` | a comment text with positional `${}` variables bound to attributes                                                          |
| `ByDefinition`    | a sentinel: use the attribute's stored definition; see [derived_attribute_definitions.md](derived_attribute_definitions.md) |

Comments are attributes of kind `COMMENT` whose expression is a `CommentTemplate`. This unification is what makes repeat
inferencing one mechanism: a condition on a comment (`C1 is in case`) and a condition on a derived value (`BMI > 30`)
are both ordinary conditions on attributes, and add/remove/replace comment are assign/retract/reassign. A replacement of
one comment by another is a change of *attribute*, because each comment text has its own attribute; a comment's text is
therefore fixed, and one name stays attached to one wording.

The alternative of a **pre-processing step** that evaluates every definition once before the rules run was rejected. It
would lose refinement (a corrected BMI for amputees is a conditioned child rule, which is exactly RDR), it saves almost
no effort (an unconditional rule is what the user gets by giving no reason), and intermediate findings would have to be
comments, forcing the user to keep them out of the report by naming convention.

Comments and derived values differ only in presentation: the report is built from comments and never from derived
values, comments are shown in the Comments panel and derived values in their own panel, and neither appears in the case
data table.

## Inference: a fixpoint

`RuleTree.apply` runs to a fixpoint wherever a case is interpreted, including cornerstone evaluation during rule
building:

1. strip every KB-assigned value from the case, so that re-interpretation is idempotent;
2. evaluate the tree; collect the assignments of the rules that fired, resolving `ByDefinition` through the KB's
   definitions;
3. write the assignments into the latest episode;
4. stop if the assignments equal the previous pass's, otherwise repeat from 2.

Conditions on external data evaluate identically on every pass; only conditions on KB-assigned attributes can change.
There is no iteration cap: the dependency graph is kept acyclic, so termination is guaranteed, and a cap would be a
guard
for a state that cannot arise.

Conflicts between rules assigning the same attribute are resolved by the RDR structure: the leaf-most satisfied rule
wins, which is what lets a child rule override a parent's formula.

## Keeping dependencies acyclic

A rule assigning A when B is absent and a rule assigning B when A is absent would oscillate forever, so cycles are
prevented at rule-building time.

- **Nodes** are the KB-assigned attributes, derived *and* comment. Comments must be nodes: a comment given by one rule,
  a derived value conditioned on the comment, and a retraction of the comment conditioned on the derived value is a
  cycle through a comment.
- **Edges**: B depends on A if a rule assigning, removing or replacing B has a condition on A anywhere on its path from
  the root, or a value expression referring to A.

`DerivedAttributeDependencyGraph` is built from the rule tree and the stored definitions; because conditions reference
attributes by id, edge extraction is exact. Every entry point checks it: the condition suggester never offers a
condition
that would create a cycle, a typed reason that would is refused with the cycle named ("would make X depend on itself:
X → Y → X"), and a value expression or definition edit that would is refused the same way. No check is needed at commit.
The check is conservative — it aggregates per attribute across rules, so mutually exclusive rules on one attribute can
be
refused although no runtime cycle exists — which is accepted for the guarantee it gives.

## Telling a formula from a literal

Whether unquoted text is a formula is a guess about the user's intent, made conservatively (`valueExpressionFor`):

- quoted text is always a literal;
- otherwise the text is a formula only if it contains an operator and **every** name in it resolves *exactly* (case and
  punctuation aside) to an existing attribute. Nothing is created to make a formula parse: a name that is not an
  attribute is far more often a typo than a future attribute.
- if no name resolves, the text is a literal (`non-diabetic` is a value, not a subtraction);
- if some resolve and one does not, the question goes back to the user, naming the nearest attribute if one is close (
  "Did you mean `weight / height`?").

Exact resolution, not the one-edit tolerance used for names mentioned in passing: `weight` and `height` differ by one
character, and a definition is stored and applied to every later case without its text being shown again. The
attribute being defined is validated before it is created, so a refused expression leaves the KB unchanged; and an
expression naming the attribute it defines is refused as a self-reference without consulting the graph.

Over the chat the model is a transcriber: it may not correct a name or substitute a formula of its own, because when it
was allowed to it produced its instructions' BMI example for every BMI request. The one exception is accepting the
server's own "did you mean" correction, since re-sending the original would ask the same question again.

## Naming

A new comment is named `C1`, `C2`, … (smallest unused, case-insensitive). Model-proposed semantic names were dropped:
they cost model attention and latency, and renaming covers the semantic case. Renaming is safe because rules and
conditions hold the id.

## Ownership of KB-assigned attributes

- An external system cannot value a derived attribute. An incoming external attribute whose name matches one is stored
  as `<name> (external)`, deterministically, so that processing never fails and data is never dropped while the
  derived attribute keeps its name. Renaming external attributes, and the alias table it would need, are not built.
- A user-chosen derived or comment name that is already in use is refused, since a user is there to pick another.
- Derived values are interpretation artefacts, never returned to the external system.

## Not built

Derived values in earlier episodes (re-interpreting with the latest episode stripped would give them); grouping derived
attributes; assigning a value by asking the model to read a note; persisted user-controlled ordering of comments and
derived values (see `tickets/attribute_ordering_plan.md`).
