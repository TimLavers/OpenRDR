Current state
The piece responsible for suggestions is @/C:
/repos/OpenRDR/server/src/main/kotlin/io/rippledown/model/rule/ConditionSuggester.kt, invoked via
RuleSessionManager.conditionHintsForCase (@/C:
/repos/OpenRDR/server/src/main/kotlin/io/rippledown/kb/RuleSessionManager.kt:184-187). The chat-side entry point is @/C:
/repos/OpenRDR/server/src/main/kotlin/io/rippledown/kb/chat/SuggestedConditionsHandler.kt.

Key observation: the suggester is currently blind to the rule action. It takes only (attributes, sessionCase), generates
a cartesian product of predicates × attributes × signatures, filters those that hold for the case, and sorts
alphabetically (Sorter at line 118). The comment text and the action type (ChangeTreeToAddConclusion / ...Remove... /
...Replace...) are available to RuleSessionManager but never reach the suggester.

So "targeting" means threading action context through, then either ranking, filtering, or generating differently based
on it.

Approaches

1. Ranking layer (lightweight)
   Keep the existing generator; add a rank step that uses the action.

Attribute-text overlap. Tokenise the comment; boost suggestions whose attribute.name shares tokens (synonyms/stems
optional).
Action-directed sort. For Add, prefer conditions that are currently true and specific (e.g. High over IsNumeric); for
Remove, prefer conditions that distinguish this case from cornerstones that kept the comment.
Replace Sorter with a RelevanceComparator composed of these signals.
Pros: a few hundred lines, no new deps, fully testable. Cons: heuristic; the candidate set is still large.

2. Historical-rule mining (strong fit for RDR philosophy)
   In a ripple-down KB, the best predictor of a good condition for a comment is the conditions that have already been
   used with that comment.

Walk kb.ruleTree, collect every rule whose conclusion equals (or is semantically close to) the target comment.
Extract their Conditions; keep those that holds(sessionCase).
Present these first, clearly flagged ("used before for this comment"). Fall back to the generator for the rest.
Pros: genuinely action-specific, deterministic, testable, explainable. Cons: cold start on a fresh conclusion; needs
canonicalisation (e.g. trim/lowercase, or embeddings) if comments vary in wording.

3. Cornerstone-based discrimination (principled)
   The session already computes cornerstone cases (existing cases that would be affected by the rule). Good conditions
   are exactly those that discriminate the session case from the cornerstones that should not get the new comment.

For each candidate condition: score by holds(sessionCase) && !holds(cornerstone) frequency across current cornerstones.
Surface the highest-discriminating few at the top.
This mirrors how an expert actually reasons in ripple-down and is the strongest "targeted" signal the server already
has. Requires passing ruleSession.cornerstoneCases() into the suggester — a structural change, not a big one.

4. LLM-assisted re-ranking / generation
   The codebase already has ConditionChatService / ConditionGenerator (@/C:
   /repos/OpenRDR/server/src/main/kotlin/io/rippledown/kb/RuleSessionManager.kt:36-44) plus ConditionExpressionParser.
   Two flavours:

Rerank: pass the comment + case + top-N candidate texts to the model; ask it to pick the most justifying subset.
Generate directly: ask the model for free-form condition expressions for "why this case warrants: <comment>", parse each
via ConditionExpressionParser, discard any that fail to parse or holds(case). Merge with the deterministic generator's
output.
Pros: natural-language-aware, handles novel comments well. Cons: latency, non-determinism, needs mock plumbing for
tests (already a pattern in the codebase via setConditionParser).

5. Embedding retrieval
   Precompute embeddings of historical conclusions (and optionally of all candidate condition texts). At suggestion
   time, embed the comment and retrieve the nearest historical conclusions → surface their conditions (feeds approach 2
   when wording differs).

Pros: robust to phrasing; fast once indexed. Cons: new infra (store + embedder); another dependency on an external
model.

6. Pipeline redesign (the "significant" option you allowed)
   Model suggestions as a composable pipeline, driven by the action:

ActionContext(case, action, cornerstones, kb) ──► SuggestionStrategy*
│
┌─ DeterministicGenerator (current code)
├─ HistoricalRuleMiner (approach 2)
├─ CornerstoneDiscriminator (approach 3)
├─ LLMReranker / LLMGenerator (approach 4)
└─ EmbeddingRetriever (approach 5)
▼
Ranker → Tierer → List<Suggestion>
Concretely:

Change ConditionSuggester to take SuggestionContext(case, action, cornerstones, kb) rather than (attributes, case).
Each strategy returns List<ScoredSuggestion> where score carries provenance (which strategy, why).
A Ranker merges, dedupes, caps. The chat handler presents tiers ("most relevant" / "also possible").
Strategies are independently unit-testable; the whole pipeline is integration-tested.

7. UI refinement (complementary)
   Regardless of server strategy: group suggestions by relevance tier in the chat panel, collapse "other suggestions"
   behind a disclosure. This makes even a modest server-side win feel much more targeted.

Recommendation
Phase the work:

Phase 1 (low risk, high yield). Thread RuleTreeChange (or at least the comment text + action type) into
ConditionSuggester. Implement approach 2 (historical mining by exact conclusion match) + approach 3 (cornerstone
discrimination, reusing ruleSession.cornerstoneCases()). Replace Sorter with a relevance-aware ranker.
Phase 2. Refactor into the pluggable pipeline of approach 6 so strategies can be added/removed.
Phase 3 (optional). Add LLM rerank/generator and/or embedding-based conclusion matching, mocked the same way
ConditionParser is mocked for tests.
Phase 1 alone removes most of the irrelevant suggestions that prompted the feature request, and it does so with
deterministic, unit-testable code. The subsequent phases are optional and can be driven by real user feedback once Phase
1 is in place.

