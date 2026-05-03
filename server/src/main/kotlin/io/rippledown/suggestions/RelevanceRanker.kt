package io.rippledown.suggestions

import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.suggestions.scorer.HistoricalRuleScorer
import io.rippledown.suggestions.scorer.ScoredSuggestion

/**
 * Orders generated suggestions by Phase 1's three deterministic signals,
 * falling back to alphabetic order for a stable tiebreak.
 *
 * Ordering (all `desc` except the final tiebreak):
 *  1. `historicalScore`     — rules that previously used this condition for
 *                             the action's target conclusion (strongest).
 *  2. `commentOverlapScore` — overlap between comment tokens and condition
 *                             tokens (added in a later commit).
 *  3. `discriminationScore` — cornerstones this condition would filter out
 *                             (added in a later commit).
 *  4. `asText()` ascending  — preserves the behaviour of the previous
 *                             `Sorter` as a deterministic final tiebreak.
 *
 * Commits 3 and 4 of Phase 1 will populate the other two signals; until
 * then they are 0 and the ranker degrades to `historical → alphabetic`,
 * which is still a strict superset of the old alphabetic-only order.
 */
internal class RelevanceRanker(ctx: SuggestionContext) {

    private val historical = HistoricalRuleScorer(ctx)

    fun rank(candidates: Collection<SuggestedCondition>): List<SuggestedCondition> =
        candidates
            .map { score(it) }
            .sortedWith(ordering)
            .map { it.suggestion }

    private fun score(s: SuggestedCondition) = ScoredSuggestion(
        suggestion = s,
        historicalScore = historical.score(s),
    )

    companion object {
        private val ordering: Comparator<ScoredSuggestion> =
            compareByDescending<ScoredSuggestion> { it.historicalScore }
                .thenByDescending { it.commentOverlapScore }
                .thenByDescending { it.discriminationScore }
                .thenBy { it.suggestion.initialSuggestion().asText() }
    }
}
