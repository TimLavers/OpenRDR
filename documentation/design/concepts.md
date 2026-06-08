# Concepts (Derived Attributes)

> Status: **Design discussion / proposal.** No implementation yet. This document
> captures the idea and the design options discussed, so that a direction can be
> chosen before any code is written.

## 1. The idea

Allow the user to add a **concept** to a project. A concept is a named formula or
algorithm whose parameters are other attribute values. Once defined, a concept:

- is **evaluated each time a case is interpreted**;
- **appears as an attribute** in the case;
- can be **used in a rule (or in another concept)** just like any other attribute;
- can be **viewed** by the user at any time (by asking the LLM for its definition);
- can be **edited**;
- can be **removed**, provided no other concept or rule uses it.

Concept definitions are stored in the project's database.

### Motivating examples

- **Numeric**: `BMI = Mass / (Height ^ 2)`.
- **Textual / semantic**: `diabetic` is true if the clinical note says
  "this patient has diabetes" or "T2DM", but **false** for "family history of
  diabetes" or "FM diabetes".

The textual example is important: it is not arithmetic, it is **text
classification with clinical negation/context exclusion**, which is a materially
harder problem than a numeric formula.

## 2. Key insight: a concept is a *derived attribute*

A concept is essentially an attribute whose value is **computed** from other values
rather than supplied in the raw case data. If we model it that way, the rest of the
engine can treat it identically to a normal attribute with almost no change to the
core model.

### How this maps onto the existing architecture

- `Attribute` (`common/.../model/Attribute.kt`) is just `(id, name)`, identified by id.
- `RDRCase` (`common/.../model/RDRCase.kt`) is a map of
  `Event(attribute, date) -> Result(Value, ReferenceRange?, units?)`, multi-episodic
  over `dates`. Values are text but expose `Value.real` for numeric use.
- Interpretation is a single seam: `KB.interpret(case) = ruleTree.apply(case)`
  (`server/.../kb/KB.kt`), and conditions read values via `case.values(attribute)`.
- Conditions already track their attribute dependencies via `attributeNames()` and
  realign to live attributes via `alignAttributes(idToAttribute)`
  (e.g. `EpisodicCondition`, `SeriesCondition`). `ConditionManager`
  (`server/.../kb/ConditionManager.kt`) realigns conditions to live attributes on load.
- Persistence is **per project**: each `KB` wires its own stores from `PersistentKB`,
  mirrored by managers (`AttributeManager`, `ConditionManager`, ...).

Because conditions and rules reference attributes **by id**, a concept-backed attribute
"just works" in rules and in other concepts' expressions with **zero change** to
`Condition` or `RuleTree`.

## 3. Proposed design

### 3.1 Model a `Concept` as a definition layered on top of a normal `Attribute`

Do **not** change `Attribute`. A `Concept` holds:

- `id`
- the backing `attributeId` (a normal attribute, created via the existing
  `AttributeManager`)
- the `definition` (its kind-specific payload — see below)
- the resolved set of **dependency attribute ids** (derived from the definition)

### 3.2 Add a `ConceptStore` / `ConceptManager` pair

Analogous to `ConditionManager`, wired into `KB` and `PersistentKB`. Responsibilities:

- per-project storage of concept definitions;
- realignment of dependencies to live attributes on load;
- cycle detection and topological ordering of concepts.

### 3.3 Inject computed values during interpretation

Add a `ConceptEvaluator` step inside `KB.interpret`/`viewableCase` that, **before**
`ruleTree.apply`, evaluates each concept and writes extra `Event/Result` entries
(via `RDRCaseBuilder`) to produce an enriched `RDRCase`. Evaluation is **per
episode/date** so concepts remain compatible with `SeriesCondition` and
`EpisodicCondition`.

### 3.4 Dependency ordering

Build a dependency DAG from each definition's referenced attribute/concept names.
Detect cycles at **definition time**; evaluate in topological order at **interpret
time** so a concept may depend on another concept.

### 3.5 Deletion safety = dependency scan

Refuse removal if:

- any rule condition's `attributeNames()` references the concept's backing attribute, or
- any other concept's definition references it.

The primitives for this already exist.

## 4. Evaluation language

This is the part that was explicitly "up for grabs". The textual requirement
(the `diabetic` example) is decisive: **no single simple grammar covers both
numeric formulas and clinical text classification well.**

### 4.1 Options considered

- **EvalEx** — lightweight, sandboxed, numeric-only. *Dropped* once textual
  concepts became a requirement.
- **Apache Commons JEXL** — sandboxed; adds conditionals, strings, and a regex
  match operator (`=~`). Covers numeric **and** simple lexical text matching in one
  grammar, and is easy for an LLM to emit and for us to parse-and-validate.
- **Kotlin scripting (JSR-223)** — maximum power, but security/sandboxing concerns,
  slow warmup, and much harder for an LLM to emit safely. **Not recommended.**

### 4.2 The textual problem is harder than regex

JEXL can express the *easy* cases as include/exclude regex, e.g.:

```
value(Note) =~ '(?i)\b(t2dm|type ?2 diabetes|diabetes mellitus|diabetic)\b'
  && !(value(Note) =~ '(?i)(family (history|hx)|fm)\W+\w*\W*diabet')
```

But lexical include/exclude lists are **brittle** for real clinical language:
"no history of diabetes", "diabetes ruled out", "?diabetes", or a large distance
between "family history" and "diabetes" all defeat regex quickly. Robust clinical
text classification is a known-hard NLP problem (the field uses NegEx/ConText-style
algorithms, or now LLMs, precisely because regex is not enough).

## 5. The deeper realisation: textual concepts are what RDR is *for*

"`diabetic` is true for 'patient has diabetes' and 'T2DM', but false for
'family history of diabetes'" is **exactly** the incremental, exception-driven
refinement that RDR was invented to capture.

This suggests a **concept kind backed by its own little rule tree** (a sub-classifier):

- Start with one rule: *note matches `diabetes` -> diabetic = true*.
- The first time "family history of diabetes" misfires, add an exception rule the
  same way any rule is built today, with the LLM helping author the condition.

Advantages over a one-shot LLM classifier:

- **Determinism preserved** — no caching hacks; cornerstones stay meaningful.
- **Explainability** — the user can ask "why is this case diabetic?" and get a rule
  trace, not a black box.
- **No new evaluation engine** — reuses `RuleTree`, `Condition`, `ConditionManager`,
  and the existing chat rule-builder. The only likely new primitive is a robust
  "text matches pattern" episodic predicate over a note attribute.
- **Clean recursion** — a concept *is* a mini-KB, and concepts can depend on concepts.

Cost: more authoring effort up front than typing one natural-language sentence, and
it can only express logic phrasable as conditions over text/values.

## 6. The design space, organised by determinism

A polymorphic `Concept` (a sealed type with a `kind`) can hold all of these behind a
single evaluator interface; only the evaluator differs per kind:

| Kind                                  | Determinism       | Cost                         | Best for                                                                                    |
|---------------------------------------|-------------------|------------------------------|---------------------------------------------------------------------------------------------|
| **JEXL expression** (numeric/lexical) | Deterministic     | Cheap, instant               | `BMI`, simple keyword text matches                                                          |
| **Sub-RDR classifier**                | Deterministic     | Cheap, instant               | Semantic text concepts like `diabetic`, with explainable exceptions                         |
| **LLM classifier**                    | Non-deterministic | Expensive per interpretation | Fuzzy semantic concepts where out-of-the-box language understanding is worth the trade-offs |

### The LLM-classifier caveat

If an LLM-classifier kind is offered, it breaks two assumptions currently enjoyed:

- **Determinism** — the same case could interpret differently across runs.
  Mitigation: cache the classifier result keyed on the note text **and** concept
  version, and only re-evaluate when the source text changes.
- **Cost/latency** — running a classifier per concept per interpretation is
  expensive; caching is essential.

## 7. Governance caveat (applies to all kinds)

RDR's correctness rests on monotonic refinement against cornerstones. **Editing a
concept's definition retroactively changes the meaning of every rule that uses it**,
which can silently invalidate past cornerstone interpretations. We will want either:

- a re-validation pass over cornerstones when a concept is edited, or
- a policy that a concept edit is a knowledge change requiring review.

This argues for **versioning** concept definitions (a version stamp per definition)
so the audit trail and cornerstone re-validation remain meaningful.

## 8. Open questions

- **Episodic semantics for text.** Numeric concepts evaluate naturally per-episode.
  Do clinical notes arrive per-date like lab results (so `diabetic` is computed per
  episode), or is a note a single latest blob? This decides whether a concept value
  is a series or a scalar.
- **Authoring/validation loop.** For JEXL concepts, the LLM emits an expression and we
  parse-and-validate against known attribute names before saving. For sub-RDR
  concepts, authoring *is* the existing chat rule-builder, scoped to the concept's
  tree. Decide whether concept authoring reuses the current chat flow or gets its own.
- **Naming collisions.** A concept's backing attribute name shares the attribute
  namespace. Decide whether concepts use a reserved naming convention or simply
  coexist, and how `attributeManager.getOrCreate` should treat a name that is a concept.
- **Which kinds to support, and which is the default.**

## 9. Current recommendation

- **Default to JEXL** for numeric and simple lexical concepts (deterministic, cheap,
  LLM-friendly, fully sandboxed).
- **Implement semantic textual concepts as sub-RDR classifiers** rather than LLM calls,
  keeping the whole feature deterministic and explainable — which matters for a
  clinical knowledge-base product.
- **Treat an LLM-classifier kind as an optional escape hatch**, added behind the same
  evaluator interface if and when fuzzy semantic concepts are genuinely needed.
- Build the `Concept` framework (model, `ConceptStore`/`ConceptManager`, dependency
  ordering, deletion safety, persistence, interpretation injection) so evaluator kinds
  are **pluggable** and can be added without rework.

## 10. Decision still pending

The direction for textual concepts — **sub-RDR classifier** (deterministic,
explainable, more authoring effort) versus a **direct LLM classifier** (less effort,
non-deterministic) — has not yet been decided.
