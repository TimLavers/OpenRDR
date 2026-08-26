# Repeat Inferencing via Derived Attributes and Comments

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

Similarly, a user may build a rule that adds, removes, or replaces a comment based on other comments that are present
(or absent) in the case.

## The model: derived attributes

In production, a case originates from an external information system — for
example a laboratory information system (LIS) — which assigns its
attributes and values. These are called *external attributes*. We model the
action of the knowledge base as *adding more attributes to the case*, whose
values are assigned by the knowledge base rather than the external system.
These are called *derived attributes*.

A derived attribute is added to the case (or removed or replaced) by a rule, i.e. in the same way as a comment is given.

Once a derived attribute has been assigned a value and added to the case, it
is indistinguishable from an externally assigned attribute for the purposes of condition evaluation, with one
difference: the knowledge base only assigns values in the most recent episode of the case. (This may change in the
future, i.e. we may want the KB to assign values to previous episodes.)

Some examples:

- `Diabetes status = "diabetic"`
- `Risk score = 7`
- `BMI = weight / height ** 2`

Both the attribute (user-named at rule-building time) and the value expression (a literal or a formula) are chosen by
the user.

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

- Fixpoint inference, stratification, reset semantics, and KB-ownership
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

The expression language for formulas is deliberately small at first: arithmetic on the latest values of attributes
(`+ - * / ** ^`, parentheses, numeric literals). Functions, episode indexing, and text
manipulation can be added later if
needed.

Telling a formula from a literal is a guess about what the user meant, so
`valueExpressionFor` makes it conservatively. Quoted text is always a literal. Otherwise the text is offered to the
formula parser only if it contains an operator character, and every name in it must resolve *exactly* — differing at
most in case or punctuation — to an attribute the KB already has. No attribute is invented to make a formula parse: a
name that is no attribute is far more often a typo than an attribute to be filled in later, and inventing it yields a
formula that can never evaluate, with nothing to tell the user why.

Exactly, and not by `attributeForName`, which tolerates a single edit. The tolerance that is right for a name mentioned
in passing is wrong here, because a formula is stored as a definition and applied to every later case while its text is
never put in front of the user again. Attribute names one edit apart are not a remote possibility either: `weight` and
`height` differ by one character, and a KB holding both once computed `weight / (weight * weight)` from a formula whose
author wrote `height`. A near-match is offered as a correction instead, never taken.

Which names resolve is asked of every name the text uses, not of the ones the parse managed to reach. The parser stops
at the first name that does not resolve, so reading the answer off the parse made `age / weight` a literal and
`weight / age` a question — the same expression either way round. `namesInFormula` tokenises the text independently for
this.

That leaves two failure modes, distinguished by whether *any* name resolved:

- No name resolved, so the text was never a formula. `non-diabetic` is a value, not a subtraction, and becomes a literal
  with nothing created.
- Some names resolved and one did not, which is genuinely ambiguous. Both readings would mislead if guessed at, so the
  reading is put back to the user to confirm — naming the nearest attribute if one is close ("Did you mean
  `weight / height`?"), otherwise offering the text ("Do you want to assign the text `weight / age`?").
  `nearestAttributeName`
  can afford a looser threshold than resolution does, since it only asks.

Asking the user to confirm only works if the answer can then be acted on, so a refused assignment must leave the KB
exactly as it was: `startRuleSessionToAssignValue` validates the expression *before* creating the attribute it defines.
Creating it first left a derived attribute with no rule and no definition, and the name then clashed with the very
request the user had just been asked to confirm — the question could be asked but not answered. Attributes are never
deleted, so the only way to not create one is to not create it yet.

The name being defined is the one name in an expression that can be unresolved for a good reason: it may not exist yet.
An expression naming it is neither a typo nor a literal but a self-reference, and needs no dependency graph to detect,
since a definition mentioning its own attribute is circular by inspection. `valueExpressionFor` is therefore told the
name being defined, and refuses such an expression with the same cycle wording used everywhere else.

None of this ladder means anything unless the expression reaches the server as the user wrote it, so over the chat the
model is a transcriber: the instructions forbid it to correct a name, rewrite an operator, or substitute a formula of
its own. It is not that the model corrects badly — asked for `weight/hieght^2` it produced `weight / (height * height)`,
which is the formula the instructions themselves used as their BMI example, for any BMI request whatever the user typed.
Tolerance for misspelling is not thereby lost, only moved to where it can be stated and tested: within a formula every
name that does not resolve exactly is put to the user, with the nearest attribute named when there is one. Nothing about
a formula is decided silently on the user's behalf. The model may send an expression other than the user's own words in
exactly one case: when the server asked "Did you mean ...?" and the user accepted, it sends the server's corrected
expression, since re-sending the original would fetch the same question and the two of them would loop.

### Comments are attributes

Report comments are themselves attributes, of a *comment* subtype.
When the user requests

> add the comment "Patient is diabetic"

the system creates a comment attribute, automatically assigns it a name,
and informs the user:

> Please confirm that you want to add the comment "Patient is diabetic".
> I have given this comment the name "C1", but you can change the name at
> any time.

The chat LLM sees the comment text and it may be able to propose a semantic name (e.g. `DiabetesStatus`), falling
back to `C1`,
`C2`, … if it can't. However, this dual-naming approach was thought to be confusing to the user, so we adopt the
approach of always naming a new attribute `CX` where X is the next available integer. The user can change it if they
want. Renaming is always safe: rules and conditions reference the
attribute id, not the name.

The report is based on the set of comment-attribute values present on the case, **not** on the derived attributes, and
in fact, this is the key distinction between them as far as the user is concerned. The AI report generator receives
these named comment attributes as its inputs; a meaningful name (if the user has assigned one) is a useful signal about
the comment's role. Comment ordering is maintained, even though it is less important as
the AI produces the report.

The existing comment actions map directly onto assignment:

- *add comment* → assign a value to a new comment attribute
- *remove comment* → refinement rule retracting the assignment
- *replace comment* → child rule assigning the comment attribute for the replacement text, with leaf-most suppression
  retracting the original

Each comment text has its own attribute, so a replacement is a change of attribute rather
than a new value for the one attribute. A comment's text is therefore fixed — changing what a comment says is adding a
different comment — which keeps a name attached to one wording, and lets a text already in the knowledge base be reused
rather than duplicated.
`RuleTreeChange` allows a replacement across two attributes only when both are comment attributes.

Conflicts between rules assigning the same attribute are resolved by the
existing RDR refinement structure: the leaf-most satisfied rule for an
attribute wins, exactly as `Rule.apply` works today for conclusions.

Comment values continue to support the existing `${}` attribute (or derived attribute)
variable placeholders (see `Interpretation.toComments`).

### Presentation

- Comment attributes appear in the collapsible Comments panel, each with its name;
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

`RuleTree.apply` currently formerly made a single depth-first pass. This changes to
a fixpoint iteration, applied wherever a case is interpreted (`KB.interpret`,
including cornerstone evaluation during rule building):

1. Strip all derived and comment attributes from the case (see reset semantics
   below).
2. Evaluate the tree against the case; collect the derived and comment attribute
   assignments made by the rules that fired.
3. Write those assignments into the latest episode of the case.
4. If the assignments are unchanged from the previous pass, stop. Otherwise repeat from step 2, **with no hard cap of
   passes as this is guaranteed to terminate**.

Conditions on external data (i.e. the original case data) evaluate identically on every pass; only conditions on
KB-assigned attributes can change
value between passes. Because the dependency graph is kept acyclic (next section), the iteration is guaranteed to
converge.

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

- Nodes: the attributes assigned by the KB — derived attributes *and*
  comment attributes.
- Edges: node B *depends on* node A if some rule whose action assigns B (or removes/replaces an assignment of B) has a
  condition referring to A anywhere on its path from the root, or has a value expression referring to A.

Comment attributes are nodes because a comment is assigned by a rule like any other value, so it can be
depended upon as well as depend on others, and a cycle through one oscillates just the same. For example: a rule gives a
comment, a rule assigns a derived value conditioned on that comment, and a rule retracting the comment is conditioned on
that derived value — so the comment is present on one pass and absent on the next, forever. Restricting the nodes to
derived attributes would leave that unprevented.

Because conditions reference attributes by id, and the set of KB-assigned
attributes is known, edge extraction from the rule tree is exact.

### Build-time prevention

Cycles are prevented during rule building, before they can reach the rule. A condition on a KB-assigned attribute *would
create a cycle* if adding its
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

- Derived attributes are flagged as KB-assigned, distinguishing them from external attributes (attributes that are
  present in the original processed case)
- The external system cannot therefore supply values for derived attributes. If an incoming case
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

## Terminology

1. **A comment is a type of derived attribute**. One rule action kind
   for the whole system: assign a value to a derived attribute.
2. **Derived attribute ordering**: same process as for external attribute ordering. A new derived attribute will be last
   on the list, but the user can change this.
3. **Episodic derived attributes**: deferred. Scenarios where derived
   values in earlier episodes would be useful are imaginable (e.g.
   conditions like `previous Diabetes status is "diabetic"`, or trends in
   a derived quantity such as BMI), and the model leaves room for them as derived values are ordinary values in
   episodes. Not for the first implementation however. When we do this, we will need to consider the rules associated
   with the derived attribute, and how they should be applied to previous episodes, e.g. by successively stripping the
   most recent episode and then re-interpreting the case.
4. **"Assignment" is the word for what a rule does, not "Conclusion"**: the rule action is
   `AssignValue(attribute, expression)`, and an interpretation is the set of `assignments()` made by the rules that
   fired. It is deliberately kind-agnostic: one word covers a comment and a derived value

## Alternative considered and rejected: pre-processing evaluation

An alternative to rule-driven derived attributes was considered: evaluate every
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
   gives conditioned overrides (e.g. a corrected `BMI` for amputees), conditional removal, and replacement — exactly the
   RDR machinery. Exceptions bolted onto a pre-processing step would reinvent rules.
3. **Abbreviated comments are a weaker substitute.** Comments are text; derived attributes carry numeric values, so
   `Risk score > 5`, trends, and formulas composing other derived attributes all work. A
   `"diabetic status"` comment cannot support these.
4. **The intermediate/final distinction becomes the user's problem.** With derived attributes shown in their own panel
   and excluded from report inputs, "intermediary, not report content" is *structural*. Repeat inferencing could then
   only be achieved using comments, and the user would have to decide whether a comment was actually an intermediate
   concept or something that the model should use when generating the report. This approach would therefore force a
   naming convention (e.g. a
   `#` prefix) to keep them out of the AI report — a worse cognitive load than the one removed, and fragile. Nor can
   "used as a condition in another rule" be inferred to mean "not for the report": many true intermediate comments may
   be given for a case yet be irrelevant to the final report (e.g. detailed GP-directed comments all suppressed when the
   referrer is a specialist).

Conclusion: Derived attributes remain rule-driven and user-visibly distinct from comments (the report is based on
comments only), while still unifying the machinery underneath.

## Out of scope

- Grouping derived attributes into folders.
- Assigning the value of a derived attribute using an AI rather than with a rule ("please read the clinical notes and
  assign diabetic status to be "diabetes" if indicated")
- Referring to derived attributes from *other* knowledge bases.
- Historical derived values in earlier episodes (see resolved decision).
- Conditions on report structure (e.g. that would set the ordering or filtering of report sections).