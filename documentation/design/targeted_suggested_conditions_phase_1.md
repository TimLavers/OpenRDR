# Targeted Suggested Conditions — Phase 1 detailed plan

Detailed implementation plan for Phase 1 of the staged work described in
`targeted_suggested_conditions.md`. Phases 2 and 3, and the Q&A around
`ExpressionCondition` / ASTs, live in their own documents.

## Goal

Thread the rule's action context (target conclusion + comment text) and the
session's cornerstones into `ConditionSuggester`, then layer three
deterministic, action-aware scorers on top of the current generator,
replacing the alphabetic `Sorter` with a relevance-aware ranker.

No pipeline refactor, no LLM, no new infrastructure. The wire shape of
`ConditionList` is unchanged — only the order of suggestions changes.

## Scope

In:

- New `SuggestionContext` carrying action, cornerstones and rule tree into
  the suggester.
- Four scorers: `HistoricalRuleScorer`, `CommentTokenOverlapScorer`,
  `CornerstoneDiscriminationScorer`, `OutOfRangeScorer`.
- A `RelevanceRanker` replacing `Sorter` at
  `server/src/main/kotlin/io/rippledown/model/rule/ConditionSuggester.kt:118`.
- A **hard cap of 20 suggestions** applied after ranking — see
  "Suggestion cap" below.
- Plumbing: `RuleSessionManager.conditionHintsForCase` populates context
  from the active session.
- Unit tests for each scorer plus integration tests through
  `RuleSessionManager`.
- Cucumber acceptance specification covering the three scorers and the
  cap — see
  `cucumber/src/test/resources/requirements/conditions/Targeted Suggested Conditions Phase 1.feature`
  with new ordering steps in
  `cucumber/src/test/kotlin/steps/SuggestionOrderingStepDefs.kt`.

Out (deferred to Phase 2 / 3):

- Pluggable strategy pipeline (Phase 2).
- **Extraction into a standalone `suggestions` module** (Phase 2 — see
  "Module boundary" below).
- Fuzzy / embedding conclusion matching, synonyms, stems (Phase 3).
- LLM rerank or generation (Phase 3).
- `ExpressionCondition` (Phase 3a).
- UI tiering of "most relevant" / "also possible" — list shape stays flat.

## Module boundary

Phase 1 code lives in `server`, under a **dedicated package** chosen to
make later extraction a mechanical move:

```
server/src/main/kotlin/io/rippledown/suggestions/
    SuggestionContext.kt
    ConditionSuggester.kt         // moved from io.rippledown.model.rule
    RelevanceRanker.kt
    scorer/
        HistoricalRuleScorer.kt
        CommentTokenOverlapScorer.kt
        CornerstoneDiscriminationScorer.kt
        OutOfRangeScorer.kt
```

Supporting classes that are currently in `io.rippledown.model.rule` and
are *only* relevant to suggestions — `SuggestedCondition`,
`EditableCondition` and friends — stay where they are for Phase 1; moving
them is Phase 2 work.

### Why not extract now

- **Type gravity sits in `server`, not `common`.** `RuleTree`,
  `RuleTreeChange`, `RuleBuildingSession`, `SuggestedCondition` all live
  in `server`. A clean `suggestions` module would require promoting them
  to `common` (or a new shared module) first — a larger refactor than
  Phase 1 itself, for no Phase 1 benefit.
- **Phase 2 is a pipeline redesign.** Extracting before Phase 2 locks in
  a public API we are about to rewrite. Extract *as part of* Phase 2,
  when the strategy SPI is the thing being designed anyway.
- **Phase 1 surface is small** (~500 LoC, one context, three scorers, one
  ranker). Well within `server`'s tolerance.
- **YAGNI.** No second consumer of the suggester exists today.

### Why the package move is still worth doing in Phase 1

The package rename (`io.rippledown.model.rule` → `io.rippledown.suggestions`
for suggester-specific types) costs essentially nothing — one directory,
updated imports — and makes the Phase 2 extraction a `git mv` plus a
Gradle module declaration rather than a cross-package untangling. Cheap
insurance.

### Phase 2 extraction outline (for context, not for implementation now)

1. Promote `RuleTree`, `RuleTreeChange`, `SuggestedCondition` and the
   editable-condition hierarchy to `common` (or a new `model-rule`
   module) — many of these already belong in `common` on their merits.
2. Create a `suggestions` module depending on `common` only.
3. Move `io.rippledown.suggestions.*` into it.
4. `server` depends on `suggestions`; `hints` / `llm` / `chat` stay out
   of the picture until Phase 3.
5. Phase 3 adds a `suggestions-llm` module depending on `suggestions` +
   `hints` / `llm`, keeping the deterministic core free of LLM
   dependencies.

## Architectural sketch

```
RuleSessionManager
  └─ conditionHintsForCase(case)
       └─ buildSuggestionContext()                      // NEW
            ├─ kb.attributeManager.all()
            ├─ kb.ruleTree
            ├─ ruleSession?.action                      // exposed (NEW)
            └─ ruleSession?.cornerstoneCases()
       └─ ConditionSuggester(context)
            ├─ generate()        // existing logic, unchanged
            ├─ score()           // NEW: list of Scorers
            └─ RelevanceRanker.rank(scored)             // NEW
```

`SuggestedCondition` itself is unchanged; scoring is internal to the
suggester. The ranked `List<SuggestedCondition>` flows out exactly as
today, so no API or serialisation change.

## Step-by-step

### 1. Expose the action on the active rule session

File: `server/src/main/kotlin/io/rippledown/model/rule/RuleBuildingSession.kt`

- Promote the existing `action: RuleTreeChange` constructor parameter to a
  read-only property.

### 2. Introduce `SuggestionContext`

New file:
`server/src/main/kotlin/io/rippledown/model/rule/SuggestionContext.kt`

```kotlin
data class SuggestionContext(
    val sessionCase: RDRCase,
    val attributes: Set<Attribute>,
    val action: RuleTreeChange?,          // null when called outside an active session
    val cornerstones: List<RDRCase>,      // empty when no session / no cornerstones
    val ruleTree: RuleTree
)
```

`action` and `cornerstones` are nullable / empty so existing call sites
that don't have a session keep working with degraded ranking — they fall
back to alphabetic order, matching today's behaviour.

### 3. Refactor `ConditionSuggester`

File: `server/src/main/kotlin/io/rippledown/model/rule/ConditionSuggester.kt`

- Change to `class ConditionSuggester(private val ctx: SuggestionContext)`.
- Keep the current generation logic verbatim — it operates on
  `ctx.sessionCase` and `ctx.attributes`.
- Replace `.sortedWith(Sorter())` at line 30 with
  `RelevanceRanker(ctx).rank(...)`.
- Delete `Sorter` (its alphabetic role moves into the ranker as the final
  tiebreak).

### 4. Scorers

New file:
`server/src/main/kotlin/io/rippledown/model/rule/SuggestionScorers.kt`

```kotlin
internal data class ScoredSuggestion(
    val suggestion: SuggestedCondition,
    val historicalScore: Int,           // historical rules that used this condition for the target conclusion
    val commentOverlapScore: Int,       // intersection size of comment tokens with condition tokens
    val discriminationScore: Int,       // cornerstones this condition excludes
    val outOfRangeScore: Int            // 1 if the candidate's attribute is low/high in the session case, else 0
)

internal interface SuggestionScorer {
    fun score(s: SuggestedCondition): Int
}
```

#### 4a. `HistoricalRuleScorer` (approach 2)

Inputs: `ctx.ruleTree`, `ctx.action`.

- Resolve `targetConclusionId`:
    - `ChangeTreeToAddConclusion` → `toBeAdded.id`
    - `ChangeTreeToRemoveConclusion` → `toBeRemoved.id`
    - `ChangeTreeToReplaceConclusion` → `replacement.id` (the comment being
      *added*; we want conditions historically used to justify that text)
    - null action → score 0 for all candidates.
- Walk `ctx.ruleTree` via `Rule.visit { ... }` and collect every rule whose
  `conclusion?.id == targetConclusionId`.
- Score for a candidate `s`: number of historical rules whose `conditions`
  contain a condition with `it.sameAs(s.initialSuggestion())`.
- `Rule.conditions` are the rule's *own* conditions, not the path. That
  matches "conditions historically used with this comment". Including
  `conditionTextsFromRoot()` is Phase 3 territory.

Edge cases:

- `targetConclusionId == null` → all zero; ranker degrades to overlap then
  discrimination then alphabetic.
- Cold start (no historical rules for this conclusion) → all zero;
  ranker leans on overlap + discrimination.
- Remove asymmetry: surfaces the conditions that gated the comment in.
  Their *negation* is what the user typically wants, but we don't try to
  invert in Phase 1; we just expose them as a positive signal so the
  user can see what their new rule is competing with.

#### 4b. `CommentTokenOverlapScorer` (approach 1)

Inputs: `ctx.action` (for the comment text), the candidate condition's
attribute name and a small predicate / structural vocabulary.

Algorithm:

1. Tokenise the comment from `ctx.action`:
    - `ChangeTreeToAddConclusion.toBeAdded.text`
    - `ChangeTreeToReplaceConclusion.replacement.text`
    - `ChangeTreeToRemoveConclusion.toBeRemoved.text`
    - null action → score 0 for all.
2. Lowercase, split on non-alphanumerics, drop a small stopword list:
   `is`, `the`, `a`, `an`, `of`, `to`, `in`, `for`, `with`, `and`, `or`,
   `not`, `are`, `was`, `were`, `be`, `been`.
3. Compute the candidate's token set via `tokensFor(condition)` — see
   table below.
4. Score = `|commentTokens ∩ conditionTokens|`.

`tokensFor(condition)` token sources:

| Condition kind                                       | Tokens contributed                                   |
|------------------------------------------------------|------------------------------------------------------|
| `EpisodicCondition(attr, IsHigh, …)` / `Range(High)` | `attr.name` + `"high"`                               |
| `… IsLow` / `Range(Low)`                             | `attr.name` + `"low"`                                |
| `… Normal` / `Range(Normal)`                         | `attr.name` + `"normal"`                             |
| `ExtendedHighRangeSuggestion`                        | `attr.name` + `"high"`                               |
| `ExtendedLowRangeSuggestion`                         | `attr.name` + `"low"`                                |
| `ExtendedHighNormalRangeSuggestion`                  | `attr.name` + `"high"`, `"normal"`                   |
| `ExtendedLowNormalRangeSuggestion`                   | `attr.name` + `"low"`, `"normal"`                    |
| `GreaterThanOrEqualsSuggestion`                      | `attr.name` + `"high"`, `"above"`, `"greater"`       |
| `LessThanOrEqualsSuggestion`                         | `attr.name` + `"low"`, `"below"`, `"less"`           |
| `IsNumeric` / `IsNotNumeric`                         | `attr.name` + `"numeric"`                            |
| `Is(value)` (`IsSuggestion`)                         | `attr.name` + tokenised `value`                      |
| `Contains` / `DoesNotContain`                        | `attr.name` + tokenised value when available         |
| `SeriesCondition(attr, Increasing)`                  | `attr.name` + `"increasing"`, `"rising"`, `"trend"`  |
| `SeriesCondition(attr, Decreasing)`                  | `attr.name` + `"decreasing"`, `"falling"`, `"trend"` |
| `CaseStructureCondition(IsPresentInCase)`            | `attr.name` + `"present"`                            |
| `CaseStructureCondition(IsAbsentFromCase)`           | `attr.name` + `"absent"`, `"missing"`                |
| `CaseStructureCondition(IsSingleEpisodeCase)`        | `"single"`, `"episode"`                              |

Signature tokens (`Current`, `All`, `AtLeast(n)`, `AtMost(n)`, `No`) are
**excluded**: they pollute scores ("at least 1" matches comments
containing "at"), and users rarely phrase comments in those terms.

The token map is a single `private fun tokensFor(condition: Condition):
Set<String>` in `CommentTokenOverlapScorer.kt`, dispatching on the sealed
`Condition` hierarchy. The map deliberately does **not** route through
`Condition.asText()` — that text is for users, varies across wordings, and
would couple ranking to display strings.

#### 4c. `CornerstoneDiscriminationScorer` (approach 3)

Inputs: `ctx.cornerstones`, `ctx.sessionCase`.

- Every candidate already satisfies `holds(sessionCase)` (filtered by
  `createSuggestions`).
- Score = count of cornerstones for which
  `condition.holds(cornerstone) == false`.
- Higher = more discriminating from cornerstones the rule shouldn't fire
  on.
- Empty cornerstones → score 0 for all; ranker falls back to other
  signals.

#### 4d. `OutOfRangeScorer`

Inputs: `ctx.sessionCase`.

A pure tiebreak scorer. When the upstream signals leave a set of
candidates equivalent, candidates whose attribute is out of reference
range in the session case rank above ones whose attribute is in range.
The intent is: a rule is almost always more interesting on the
abnormal attribute (e.g. AST high) than on the normal one sitting
alongside it (e.g. ALT normal).

- Score `1` if `case.getLatest(attr).isLow() || .isHigh()` for the
  candidate's attribute.
- Score `0` for normal values, attributes without a reference range,
  non-numeric values, and conditions with no meaningful attribute
  (e.g. `IsSingleEpisodeCase`).
- The score is per-attribute, not per-predicate: every condition
  referencing an out-of-range attribute inherits the bonus. This
  avoids special-casing predicate shapes and keeps the scorer
  agnostic of the predicate taxonomy.
- Results are cached per attribute in the scorer's constructor — a
  single case lookup per attribute covers every candidate.

The scorer is deliberately placed **below** the three original signals
in the ordering: historical / comment / discrimination all encode
stronger intent. Out-of-range only fires when those are tied.

### 5. `RelevanceRanker` and the suggestion cap

Internal, four signals ordered by reliability:

```
primary     : historicalScore     desc   // strongest, action-conclusion specific
secondary   : commentOverlapScore desc   // works at cold start
tertiary    : discriminationScore desc   // case / cornerstone specific
quaternary  : outOfRangeScore     desc   // abnormal-attribute tiebreak
final tie   : asText()            asc    // deterministic, regression-safe
```

Rationale for ordering:

- **Historical first** — a condition the KB has used before for this
  exact conclusion is the strongest possible signal in an RDR setting.
- **Overlap second** — when there's no history, the attribute and
  direction the user just typed about are almost always more relevant
  than an accidental discrimination win.
- **Discrimination third** — shines when several candidates already
  cluster on the right attribute and we need to pick among them.
- **Out-of-range fourth** — when the first three tie, prefer the
  attribute that is actually abnormal in the case. Pure tiebreak; it
  never overrides an explicit comment / historical signal.
- **Alphabetic last** — preserves today's ordering as a stable tiebreak;
  keeps tests deterministic.

#### Suggestion cap

After ranking, take the top **20** suggestions and drop the rest. The
cap exists for two reasons:

- **UX.** The chat UI shows ~5 suggestions by default, expandable to
  ~10. Twenty is roughly four pages of expansion — the realistic
  scrollable maximum. Anything beyond is read by no one.
- **LLM resolution.** `SuggestedConditionsHandler` formats the list for
  the bot to map back from "the third one" / "MCV high" to a concrete
  suggestion. Twenty entries is meaningfully easier to disambiguate
  than fifty.

The cap is a single `MAX_SUGGESTIONS` constant in the suggester (or on
`SuggestionContext`) so Phases 2 / 3 can tune it without touching the
ranker. Likely Phase 3 trajectory: drop to 10–15 once LLM / embedding
signals consistently push the right answer into the top 10.

The cap applies *after* ranking, so it removes the lowest-relevance
entries — never a top-ranked candidate.

### 6. Wire context through `RuleSessionManager`

File: `server/src/main/kotlin/io/rippledown/kb/RuleSessionManager.kt:184`

```kotlin
override fun conditionHintsForCase(case: RDRCase): ConditionList {
    val ctx = SuggestionContext(
        sessionCase = case,
        attributes = kb.attributeManager.all(),
        action = ruleSession?.action,
        cornerstones = ruleSession?.cornerstoneCases().orEmpty(),
        ruleTree = kb.ruleTree
    )
    return ConditionList(ConditionSuggester(ctx).suggestions())
}
```

`conditionForSuggestionText` continues to call `conditionHintsForCase`;
its lookup is by text equality, which is order-independent — no
behavioural change.

### 7. Backwards compatibility

The two-arg `ConditionSuggester(attributes, sessionCase)` is referenced
from `ConditionSuggesterTest`. Migrate the call sites to construct a
`SuggestionContext` with `action = null` and `cornerstones = emptyList()`
rather than keeping a deprecated secondary constructor — smaller surface,
no dead code.

## Tests

### New

- `HistoricalRuleScorerTest`
    - No matching historical rules → all zero.
    - One historical rule with one matching condition → score 1.
    - Multiple rules using the same condition → score N.
    - Conclusion match by id, not by reference identity (KBs reload).
    - Action types: Add uses `toBeAdded`; Replace uses `replacement`;
      Remove uses `toBeRemoved`.
- `CommentTokenOverlapScorerTest`
    - "TSH is high" → `EpisodicCondition(TSH, High, Current)` scores 2;
      same attr with `Low` scores 1.
    - "TSH normal range" → `ExtendedHighNormalRangeSuggestion(TSH)` scores
      ≥ 2, beats plain `High` candidate scoring 1.
    - "Glucose missing" → `CaseStructureCondition(IsAbsentFromCase(Glucose))`
      scores 2.
    - Predicate-only match still positive when the attribute name doesn't
      appear: "values are high across the board" → any `…High…` candidate
      scores 1.
    - Stopwords ignored (`"is"` doesn't match any predicate token).
    - Signature tokens not picked up: "at least three" does not boost
      `AtLeast(3)` over `AtLeast(1)` — regression guard.
    - `ChangeTreeToReplaceConclusion` uses replacement text, not the
      original.
    - Null action → all zero.
- `CornerstoneDiscriminationScorerTest`
    - No cornerstones → all zero.
    - Candidate excludes 0 / 1 / all cornerstones.
    - Editable suggestion uses its initial condition.
- `OutOfRangeScorerTest`
    - Attribute whose latest value is low or high scores 1; in-range
      attribute scores 0.
    - Score is per-attribute and ignores the candidate's predicate
      (`is high` on a high attribute and `increasing` on the same
      attribute both score 1).
    - Attribute without a reference range scores 0.
    - Conditions with no attribute (e.g. `IsSingleEpisodeCase`) score 0.
- `RelevanceRankerTest`
    - Historical beats overlap beats discrimination beats alphabetic.
    - All-zero → alphabetic order matches today's `Sorter` exactly
      (regression).
  - List is truncated to `MAX_SUGGESTIONS` (= 20) after ranking; kept
    entries are the top-ranked ones, dropped entries are the lowest.

### Updated

- `ConditionSuggesterTest`
    - Migrate to `SuggestionContext`. Existing assertions on the *set* of
      generated suggestions must continue to pass; assertions on order use
      the no-context constructor or accept the new ranking.
- `RuleSessionManagerTest`
    - Start an Add session against a conclusion that has an existing rule
      using `EpisodicCondition(TSH, High, Current)`; assert that suggestion
      is at index 0.
    - Cornerstone discrimination drives ordering when no historical rules
      exist.
    - Comment-token overlap drives ordering when no history and no
      cornerstones.
- `SuggestedConditionsHandlerTest`
    - No behavioural change expected — handler delegates to
      `conditionHintsForCase`. Add one test asserting the LLM-facing list
      order tracks the ranker's output (mocked `RuleService`).

## Risks and explicit non-goals

- **Conclusion identity** — historical mining keys on `Conclusion.id`
  from `kb.conclusionManager.getOrCreate(text)`. Exact-text matching;
  paraphrases miss. Acceptable for Phase 1; cold start is covered by
  overlap.
- **Remove / Replace asymmetry** — historical signal for Remove is
  informational, not prescriptive; we don't try to invert conditions.
- **Predicate-text vocabulary is small and hand-curated.** It is *not* an
  NLP layer; it captures the handful of clinical-direction words that
  recur in comments ("high", "low", "normal", "increasing", "decreasing",
  "present", "absent"). Expanding it (synonyms, stems, multi-word
  phrases) is Phase 3 territory and should be driven by real comment
  data, not by guessing.
- **Performance** — `Rule.visit` is O(rules); cornerstone discrimination
  is O(candidates × cornerstones). Both are bounded by KB size and
  already paid elsewhere; no caching needed in Phase 1.
- **No UI / protocol changes** — `ConditionList` shape is unchanged;
  tier display is Phase 7 / Phase 2.

## Suggested commit breakdown

1. Expose `RuleBuildingSession.action`; introduce `SuggestionContext`;
   move `ConditionSuggester` to `io.rippledown.suggestions` package and
   migrate its constructor (no behaviour change — alphabetic fallback
   only). Apply the 20-suggestion cap at the end of `suggestions()`.
2. Add `HistoricalRuleScorer` + tests; ranker uses it.
3. Add `CommentTokenOverlapScorer` + tests; ranker uses it.
4. Add `CornerstoneDiscriminationScorer` + tests; ranker uses it.
5. Add `OutOfRangeScorer` + tests; ranker uses it as the last signal
   before the alphabetic tiebreak.
6. Wire `RuleSessionManager.conditionHintsForCase` to populate the
   context; integration tests through `RuleSessionManager` and
   `SuggestedConditionsHandler`.
7. Remove the old `Sorter`; tighten test order assertions; wire the
   ordering cucumber steps in `SuggestionOrderingStepDefs.kt` and the
   `I work through any cornerstone cases` placeholder.

Each commit is independently green and reviewable.
