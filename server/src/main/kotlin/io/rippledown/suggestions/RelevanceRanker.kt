package io.rippledown.suggestions

import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.suggestions.scorer.*

/**
 * Orders generated suggestions by Phase 1's deterministic signals,
 * falling back to alphabetic order for a stable tiebreak.
 *
 * Ordering (all `desc` except the final tiebreak):
 *  1. `historicalScore`     — rules that previously used this condition for
 *                             the action's target conclusion (strongest).
 *  2. `commentOverlapScore` — overlap between the action comment's tokens
 *                             and the candidate's attribute / direction
 *                             tokens.
 *  3. `discriminationScore` — cornerstones this condition would filter out
 *                             (case- and cornerstone-specific tiebreak).
 *  4. `outOfRangeScore`     — candidates whose attribute is currently low
 *                             or high in the case rank above ones whose
 *                             attribute is in normal range. Pure tiebreak;
 *                             only kicks in when the upstream signals are
 *                             tied.
 *  5. `asText()` ascending  — preserves the behaviour of the previous
 *                             `Sorter` as a deterministic final tiebreak.
 */
internal class RelevanceRanker(ctx: SuggestionContext) {

    private val historical = HistoricalRuleScorer(ctx)
    private val commentOverlap = CommentTokenOverlapScorer(ctx)
    private val discrimination = CornerstoneDiscriminationScorer(ctx)
    private val outOfRange = OutOfRangeScorer(ctx)

    fun rank(candidates: Collection<SuggestedCondition>): List<SuggestedCondition> =
        candidates
            .map { score(it) }
            .sortedWith(ordering)
            .map { it.suggestion }

    private fun score(s: SuggestedCondition) = ScoredSuggestion(
        suggestion = s,
        historicalScore = historical.score(s),
        commentOverlapScore = commentOverlap.score(s),
        discriminationScore = discrimination.score(s),
        outOfRangeScore = outOfRange.score(s),
    )

    companion object {
        private val ordering: Comparator<ScoredSuggestion> =
            compareByDescending<ScoredSuggestion> { it.historicalScore }
                .thenByDescending { it.commentOverlapScore }
                .thenByDescending { it.discriminationScore }
                .thenByDescending { it.outOfRangeScore }
                .thenBy { it.suggestion.initialSuggestion().asText() }
    }
}
