# Targeted Suggested Conditions

When building a rule, the user must add conditions that are true for the
session case but false for the cases that should keep their current report.
Choosing good conditions is hard, so the system offers a ranked list of
candidate conditions. This document records the options that were considered
for making those suggestions *targeted* (aware of the rule being built) and
then describes the design as currently implemented.

## The problem

Originally the suggester was blind to the rule action. It took only
`(attributes, sessionCase)`, generated the cartesian product of
predicates × attributes × signatures, kept those that held for the session
case, and sorted them alphabetically (a `Sorter`). The comment text and the
action type (`ChangeTreeToAddConclusion` / `…Remove…` / `…Replace…`) were
available to `RuleSessionManager` but never reached the suggester, so the
list was long, generic, and in no useful order.

"Targeting" means threading the action context (and the session's cornerstone
cases) into the suggester, then ranking, filtering, or generating using it.

## Options considered

1. **Ranking layer (lightweight).** Keep the existing generator, add a rank
   step that uses the action: boost suggestions whose attribute name shares
   tokens with the comment, and prefer conditions that discriminate the case
   from cornerstones. A few hundred lines, no new dependencies, fully
   testable — but heuristic, and the candidate set is still large.
2. **Historical-rule mining.** In an RDR knowledge base, the best predictor
   of a good condition for a comment is the conditions already used with that
   comment. Collect the conditions of rules whose conclusion matches the
   target comment and that hold on the session case, and surface them first.
   Deterministic, explainable, strong fit for RDR; cold-start on a brand-new
   conclusion.
3. **Cornerstone-based discrimination.** Good conditions are exactly those
   that separate the session case from the cornerstones that should *not* get
   the new comment. Score each candidate by how many cornerstones it excludes.
   Mirrors how an expert reasons; the strongest "targeted" signal the server
   already has.
4. **LLM-assisted re-ranking / generation.** Ask the model to pick the most
   justifying subset of candidates, or to generate free-form condition
   expressions that are then parsed and validated. Natural-language-aware and
   good on novel comments, but adds latency, non-determinism, and test
   plumbing.
5. **Embedding retrieval.** Embed the comment and retrieve the nearest
   historical conclusions to feed (2) when wording differs. Robust to
   phrasing, but needs new infrastructure (a store and an embedder).
6. **Pipeline redesign.** Model suggestions as a composable pipeline of
   strategies (deterministic generator, historical miner, cornerstone
   discriminator, LLM, embeddings) feeding a ranker and tierer. Flexible and
   independently testable, but a significant refactor.
7. **UI tiering (complementary).** Group suggestions into "most relevant" and
   "also possible" tiers in the chat panel. Makes even a modest server-side
   win feel more targeted.

### Decision: phase the work

- **Phase 1 (implemented).** Approaches 1 + 2 + 3 as deterministic scorers,
  with a relevance-aware ranker replacing the alphabetic `Sorter`. No LLM, no
  pipeline refactor, no new infrastructure, no protocol change.
- **Phase 2 (not built).** Refactor into the pluggable strategy pipeline of
  approach 6 and extract a standalone `suggestions` module.
- **Phase 3 (not built, optional).** Add LLM rerank/generation and/or
  embedding-based conclusion matching, driven by real usage of Phase 1.

## Current design (Phase 1)

### Where it lives

```
server/src/main/kotlin/io/rippledown/suggestions/
    SuggestionContext.kt
    ConditionSuggester.kt        // generation + injection + prunes, then ranking
    RelevanceRanker.kt           // replaced the old Sorter
    scorer/
        HistoricalRuleScorer.kt
        CommentTokenOverlapScorer.kt
        CornerstoneDiscriminationScorer.kt
        OutOfRangeScorer.kt
```

The package was deliberately separated from `io.rippledown.model.rule` so the
Phase 2 extraction into its own module is a mechanical move. The wire shape of
`ConditionList` is unchanged — only the order (and length) of the suggestions
changes.

### Flow

```
RuleSessionManager.conditionHintsForCase(case)
  └─ SuggestionContext(sessionCase, attributes, action, cornerstones, ruleTree)
       └─ ConditionSuggester(context).suggestions()
            ├─ generate()                 // predicate × attribute × signature
            ├─ inject historical literals
            ├─ prune low-value shapes
            ├─ score()                    // four scorers
            └─ RelevanceRanker.rank() → take(MAX_SUGGESTIONS)
```

`action` is null and `cornerstones` is empty when the suggester is called
outside an active session; the ranker then degrades cleanly to alphabetic
order, matching the original behaviour.

### Candidate generation, injection, and prunes

Candidates come from a deterministic generator that enumerates the three
classes of condition (see `conditions.md`) over the session case's attributes:

- **Episodic** conditions are enumerated as attribute × test-result predicate
  × signature. The signatures are restricted to match the number of episodes
  in the case — for a single-episode case only `Current` makes sense.
- **Series** conditions are enumerated as attribute × series predicate, and
  are omitted entirely for single-episode cases.
- **Case-structure** conditions are just a predicate, so they are trivial to
  enumerate.

A candidate is only offered if it can actually apply to the session case
(`shouldBeSuggestedForCase`): a non-editable condition is suggested if and
only if it holds for the case, and an editable condition is suggested unless
there is no way to edit it so that it holds (e.g. `TSH ≥ _` is not offered
when the case's latest `TSH` is non-numeric). On top of that:

**Historical-condition injection.** The literal conditions of every rule
whose conclusion id matches the action's target conclusion, and that
`holds(sessionCase)`, are injected as candidates in their own right. In
pathology the cutoffs in existing rules are usually the clinically defensible
ones (e.g. a historical `eGFR ≥ 70`), whereas the generator's editable cutoff
pins to the case's current reading (e.g. `eGFR ≥ 74`). Injecting the literal
lets the user pick the clinical cutoff in one click and lets the historical
scorer boost it via plain `sameAs`. Injected conditions are deduped against
generated ones by `sameAs`, preferring the editable form when cutoffs
coincide.

**Generator-level prunes.** A few shapes are suppressed before ranking
because they reliably waste the 20-slot budget:

- `Is(<numeric value>)` is dropped for numeric attributes (the `≥`/`≤`
  editable cutoffs and `is high`/`low`/`normal` already cover the intent).
- `Contains` / `DoesNotContain` are dropped for numeric attributes, and
  restricted to attributes that have been substring-matched in some existing
  rule (otherwise free-text fields like addresses produce noise).
- A `No <attr> is <range>` candidate is dropped when `all <attr> are <other
  range>` is also generated (the range predicates are mutually exclusive, so
  it is implied).
- `IsNumeric` / `IsNotNumeric`, `ExtendedRange` ("by at most N%"),
  `AtLeast(n)` / `AtMost(n)` signatures, and `IsPresentInCase` /
  `IsAbsentFromCase` were judged to add no clinical value and removed from the
  factory list.

### Scorers

Each scorer returns an integer per candidate (`ScoredSuggestion`):

- **`HistoricalRuleScorer`** — number of rules targeting the action's
  conclusion whose conditions `sameAs` the candidate. The strongest signal in
  an RDR setting. Add uses `toBeAdded`, Replace uses `replacement`, Remove
  uses `toBeRemoved`; a null action scores 0.
- **`CommentTokenOverlapScorer`** — size of the intersection between the
  comment's tokens and the candidate's tokens. The comment is lowercased,
  split on non-alphanumerics, and stripped of a small stopword list; the
  candidate's tokens come from a hand-curated map of attribute name plus
  direction words (`high`, `low`, `normal`, `increasing`, …). Signature tokens
  are excluded. Value tokens from `Is`/`Contains` predicates are only counted
  when the value has at most `MAX_VALUE_TOKENS` (= 3), so multi-sentence
  comment fields can't win on accidental word collisions. Works at cold start.
- **`CornerstoneDiscriminationScorer`** — number of cornerstones for which the
  candidate does *not* hold (i.e. cornerstones it would exclude from the new
  rule). Empty cornerstones → 0 for all.
- **`OutOfRangeScorer`** — a pure tiebreak: 1 if the candidate's attribute is
  low or high in the session case, else 0. Prefers a rule on the abnormal
  attribute when the stronger signals are tied.

### `RelevanceRanker` and the suggestion cap

The ranker orders candidates by the four scores, strongest first, with an
alphabetic final tiebreak:

```
historicalScore     desc   // KB has used this condition for this conclusion
commentOverlapScore desc   // attribute/direction the user just typed about
discriminationScore desc   // separates the case from the cornerstones
outOfRangeScore     desc   // abnormal-attribute tiebreak
asText()            asc    // deterministic, regression-safe
```

#### Suggestion cap

After ranking, only the top **`MAX_SUGGESTIONS` (= 20)** are kept. The chat UI
shows ~5 suggestions by default, expandable to ~10, so twenty is the realistic
scrollable maximum; it also keeps the LLM-facing list (in
`SuggestedConditionsHandler`) easy to disambiguate. The cap is applied *after*
ranking, so it only ever removes the lowest-relevance entries. It is a single
constant so later phases can tune it.

### Tests

Each scorer has unit tests, `RelevanceRankerTest` covers the ordering and the
cap, and the behaviour is pinned end-to-end by
`cucumber/src/test/resources/requirements/conditions/Targeted Suggested Conditions Phase 1.feature`
with ordering steps in `SuggestionOrderingStepDefs.kt`.

## Future directions (not yet built)

### Phase 2 — pluggable pipeline and module extraction

Turn the scorers into a strategy pipeline so strategies can be added or
removed, and extract `io.rippledown.suggestions` into its own Gradle module.
That first requires promoting `RuleTree`, `RuleTreeChange` and
`SuggestedCondition` (currently in `server`) to `common`.

### Phase 3 — free-form / LLM-generated conditions

The expressive limit today is that conditions come from a fixed vocabulary
(`ConditionExpressionParser` matches ~15 patterns, each producing one concrete
`Condition` subclass — there is no `AND`/`OR`/negation/cross-attribute
algebra). Two staged options were discussed:

- **Phase 3a — `ExpressionCondition` (recommended when needed).** Add a
  *single* new subclass to the sealed `Condition` hierarchy that holds a
  free-form expression string, parses it into a tree, and evaluates `holds`
  by walking that tree in plain Kotlin. The LLM proposes the expression text;
  it is parsed, validated, deduped by `sameAs`, and persisted via
  `ConditionManager.getOrCreate`. No compilation, no classloading, no codegen
  — the KB artefact is just a string, so it stays portable and explainable.
- **Phase 3b — dynamically compiled condition classes (speculative).** Only
  pursue if 3a shows expression evaluation is a real bottleneck or users need
  operators the DSL genuinely cannot express. It needs a compiler dependency,
  a security sandbox, source-not-bytecode persistence with a compile cache, a
  versioned facade, and per-class classloader management. Substantial
  infrastructure; **YAGNI** until 3a proves the need.

### Should the existing conditions become a generic AST?

**No.** The current `Condition` hierarchy is already an AST — a tree of typed
nodes (`EpisodicCondition` with `attribute`/`predicate`/`signature` children,
`SeriesCondition`, `CaseStructureCondition`) whose `holds(case)` is the
interpreter. The difference from a general AST is that the grammar is fixed and
domain-specific, which is exactly what gives meaningful `sameAs`, stable
serialisation, and dedicated UI editors. A refactor to a uniform
`And`/`Or`/`Compare` algebra would lose all of that for uniformity not
currently needed. If compositional free-form expressions are wanted, add
`ExpressionCondition` *alongside* the existing subclasses (Phase 3a) rather
than refactoring them.

Two risks worth keeping in mind for any expression-based variant:

- **Canonicalise expressions** before `sameAs`, or `x > 10 && y < 5` and
  `y < 5 && x > 10` persist as two different conditions and the library
  fragments.
- **LLM-authored tests validate the model's interpretation, not the human's
  intent** — useful for catching parse/compile errors, but "tests pass" does
  not mean "condition is correct".