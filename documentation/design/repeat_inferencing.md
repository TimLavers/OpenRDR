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

In production, a case originates from an external system such as a
laboratory information system (LIS), which assigns its attributes and
values. We model the action of the knowledge base as *adding more attributes
to the case*, whose values are assigned by the knowledge base rather than
the LIS. These are called *derived attributes*.

Once a derived attribute has been assigned a value and added to the case, it
is indistinguishable from an LIS-assigned attribute for the purposes of
condition evaluation, with one minor difference: the knowledge base only
assigns values in the most recent episode of the case.

There is exactly one kind of rule action: *assign a value to a derived
attribute*, e.g.:

- `Diabetes status = "diabetic"`
- `Risk score = 7`

Both the attribute (user-named at rule-building time) and the value are
chosen by the user.

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
- Non-comment derived attributes (e.g. `Risk score`) appear in the case
  view, styled as KB-derived (styling TBD).

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
   passes as this will be provably terminate.

Conditions on LIS data evaluate identically on every pass; only conditions
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
  condition referring to A anywhere on its path from the root.

Because conditions reference attributes by id, and the set of derived
attributes is known, edge extraction from the rule tree is exact.

### Build-time check

When a rule session attempts to commit a rule containing a condition on a
derived attribute, the would-be new edges are added to the graph. If a
cycle results, the commit is rejected with a message identifying the cycle,
e.g.:

> This condition cannot be used: it would make "X" depend on itself
> (X → Y → X).

The check lives in the rule session machinery on the server; the graph is
computed from the rule tree on demand (no persistence needed).

## Derived attributes are KB-owned

- Derived attributes are flagged as KB-assigned, distinguishing them from
  LIS attributes.
- The LIS cannot supply values for them; a collision (external case
  providing a value for a derived attribute) is rejected or ignored.
- Derived values are interpretation artefacts: they are never echoed back
  to the LIS, and are excluded from whatever is exported as case data.
- The case view may present non-comment derived attributes distinctly
  (styling TBD); comment attributes are shown in the Comments panel only.

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
3. **Episodes.** Derived values are written only to the latest episode.
   Conditions like `previous Diabetes status is true` would require
   persisting past interpretations — out of scope for now, but should the
   design leave room for it?

## Out of scope

- Referring to derived attributes from *other* knowledge bases.
- Historical derived values in earlier episodes (see open question 3).
- Conditions on report structure (ordering, sections).

## Testing

- Derived-attribute model tests: KB-owned flag, comment vs data subtype,
  LIS collision handling, reset/strip semantics, assignment to latest
  episode only.
- Comment-attribute tests: auto-naming and rename safety, `${}` variable
  placeholders in values, Comments panel presentation, report generation
  from named comment attributes.
- `RuleTree`/`KB` tests: multi-pass convergence, first-pass semantics of
  absence conditions, iteration cap, idempotent re-interpretation.
- Dependency-graph tests: edge construction (including inherited path
  conditions), cycle detection and rejection message.
- Rule-session tests: cornerstone behaviour with chained rules.
- Cucumber: end-to-end scenario — build a rule assigning
  `Diabetes status = "diabetic"`, then build a dependent rule conditioned
  on it, verify the dependent comment is given; a numeric-value scenario
  (`Risk score > 5`); a scenario using `is not in case`; a scenario showing
  a cyclic condition is rejected.
