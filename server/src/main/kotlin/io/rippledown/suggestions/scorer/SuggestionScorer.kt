package io.rippledown.suggestions.scorer

import io.rippledown.model.condition.edit.SuggestedCondition

/**
 * A single scored suggestion, carrying the individual signal values that
 * [io.rippledown.suggestions.RelevanceRanker] orders on.
 *
 * Scorers added in later Phase 1 commits populate the other fields; defaults
 * of 0 let each commit remain independently green.
 */
internal data class ScoredSuggestion(
    val suggestion: SuggestedCondition,
    val historicalScore: Int = 0,
    val commentOverlapScore: Int = 0,
    val discriminationScore: Int = 0,
)

internal interface SuggestionScorer {
    fun score(s: SuggestedCondition): Int
}
