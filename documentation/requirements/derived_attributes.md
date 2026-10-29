# Derived attributes

## Why

An expert who has built a rule establishing that a patient is diabetic wants later rules to say "if diabetic" rather
than repeat the raw-data conditions. A derived attribute is a value the knowledge base works out for a case and writes
onto it, so that other rules can condition on it exactly as they condition on external data. The inference engine runs
until no rule changes anything, so a finding in one rule can fire another.

Derived values share one rule-assignment mechanism:

- a **literal**, such as `Diabetes status = "diabetic"`, typically given under conditions;
- a **formula** over other attributes, such as `BMI = weight / height ^ 2`;
- an **AI Boolean** classification of one primary text attribute against approved criteria.

Formulas were previously a gap: `mass / height^2 > 28` could not be written as a condition. A condition on a
formula-derived attribute closes it.

## Requirements

| Requirement         | Description                                                                                                                                                                                                                                                                                                                                                     | Validation                                       |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------|
| Assign by rule      | A rule assigns a value or a formula to a named derived attribute. Asking for it with no reason gives an unconditional rule, so a formula "is just evaluated for every case" from the user's point of view.                                                                                                                                                      | `inferencing/Derived attribute.feature`          |
| Remove and replace  | Refinement rules retract a derived value or replace it, so a formula can have an exception (a corrected BMI for an amputee) built the RDR way, with cornerstone review.                                                                                                                                                                                         | `inferencing/Derived attribute.feature`          |
| Repeat inferencing  | A condition may refer to a derived attribute or a comment; the rules run until the interpretation is stable.                                                                                                                                                                                                                                                    | `inferencing/Repeat inferencing.feature`         |
| Missing input       | A formula whose input has no value in the case assigns nothing, so `is in case` remains meaningful downstream and formulas need no guard conditions.                                                                                                                                                                                                            | `inferencing/Derived attribute.feature`          |
| Formula language    | Arithmetic over the latest values of attributes: `+ - * / ^` (`**` also accepted), parentheses, numeric literals. Results are shown to four significant figures.                                                                                                                                                                                                | `FormulaTest`                                    |
| Literal or formula  | Quoted text is always a literal. Unquoted text is a formula only if it contains an operator and every name in it is an attribute the knowledge base already has. A name that almost matches is offered back as a question ("Did you mean `weight / height`?"), never taken silently, because a definition is applied to later cases and must reflect the expert's intent. | `inferencing/Derived attribute.feature`          |
| No cycles           | A condition or formula that would make an attribute depend on itself, directly or through other rules and comments, is refused with an explanation naming the cycle, and is never suggested.                                                                                                                                                                    | `inferencing/Repeat inferencing.feature`         |
| Edit a definition   | The user can correct a derived attribute's formula or value everywhere it is given, without building a rule and without cornerstone review. A case- or condition-specific correction is instead a replacement rule. The chat asks which is meant when the request is ambiguous.                                                                                 | `inferencing/Editing derived definition.feature` |
| Panel               | Derived values appear in their own panel under the case table, not in the table, with the formula and the rule's conditions on hover. The panel is present even when empty.                                                                                                                                                                                     | `inferencing/Derived attribute.feature`          |
| KB-owned            | External data cannot supply a derived attribute's value; a clash is stored as `<name> (external)`. Derived values are never sent back to the external system.                                                                                                                                                                                                   | `KBExternalCollisionTest`                        |
| Latest episode only | Derived values are written into the most recent episode. **Derived values for earlier episodes are not implemented.**                                                                                                                                                                                                                                           |                                                  |

**Not implemented:** deleting a derived attribute; listing all derived attributes; an impact summary when a definition
is edited; a configurable number of significant figures. See the [backlog](../tickets/TODO.md).

Design: [design/rule_tree_and_inference.md](../design/rule_tree_and_inference.md) and
[design/derived_attribute_definitions.md](../design/derived_attribute_definitions.md).

## AI Boolean definitions

An expert can define a Boolean from one primary text attribute through chat. The saved criteria, labelled
examples, profile and revision determine future classifications. A preview is accepted as an exact server-owned
draft, then committed through normal rule building. Missing or blank latest-episode input is not evaluated;
a valid false remains a value. Evaluation failures produce an incomplete interpretation and block reports.
The Derived attributes panel shows these states separately and exposes the source, criteria, saved evidence and rule conditions in its tooltip. Evaluation time is retained in provenance.
See [the diabetes demo](../ai_derived_attribute_demo.md) for a ten-case chat demonstration and its run command.
