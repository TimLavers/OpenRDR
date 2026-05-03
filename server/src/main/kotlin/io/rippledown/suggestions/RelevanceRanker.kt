package io.rippledown.suggestions

import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.suggestions.scorer.CommentTokenOverlapScorer
import io.rippledown.suggestions.scorer.HistoricalRuleScorer
import io.rippledown.suggestions.scorer.ScoredSuggestion

/**
 * Orders generated suggestions by Phase 1's three deterministic signals,
 * falling back to alphabetic order for a stable tiebreak.
 *
 * Ordering (all `desc` except the final tiebreak):
 *  1. `historicalScore`     — rules that previously used this condition for
 *                             the action's target conclusion (strongest).
 *  2. `commentOverlapScore` — overlap between the action comment's tokens
 *                             and the candidate's attribute / direction
 *                             tokens.
 *  3. `discriminationScore` — cornerstones this condition would filter out
 *                             (added in a later commit).
 *  4. `asText()` ascending  — preserves the behaviour of the previous
 *                             `Sorter` as a deterministic final tiebreak.
 *
 * Commit 4 of Phase 1 will populate `discriminationScore`; until then it is
 * 0 and the ranker degrades to `historical → comment overlap → alphabetic`.
 */
internal class RelevanceRanker(ctx: SuggestionContext) {

    private val historical = HistoricalRuleScorer(ctx)
    private val commentOverlap = CommentTokenOverlapScorer(ctx)

    fun rank(candidates: Collection<SuggestedCondition>): List<SuggestedCondition> =
        candidates
            .map { score(it) }
            .sortedWith(ordering)
            .map { it.suggestion }

    private fun score(s: SuggestedCondition) = ScoredSuggestion(
        suggestion = s,
        historicalScore = historical.score(s),
        commentOverlapScore = commentOverlap.score(s),
    )

    companion object {
        private val ordering: Comparator<ScoredSuggestion> =
            compareByDescending<ScoredSuggestion> { it.historicalScore }
                .thenByDescending { it.commentOverlapScore }
                .thenByDescending { it.discriminationScore }
                .thenBy { it.suggestion.initialSuggestion().asText() }
    }
}
