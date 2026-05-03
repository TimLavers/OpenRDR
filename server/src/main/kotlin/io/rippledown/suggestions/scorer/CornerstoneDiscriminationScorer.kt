package io.rippledown.suggestions.scorer

import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.suggestions.SuggestionContext

/**
 * Scores a [SuggestedCondition] by the number of cornerstone cases it would
 * filter *out* — i.e. cases the new rule should not fire on.
 *
 * Every candidate already satisfies the session case (the suggestion
 * generator filters by `shouldBeSuggestedForCase`), so the meaningful
 * signal is how many cornerstones it excludes. Higher scores correspond
 * to more discriminating conditions.
 *
 * Empty cornerstones (no active session, or a session whose every
 * cornerstone is already exempted) → 0 for every candidate, and the
 * ranker degrades to the other signals.
 *
 * Editable suggestions are scored against their `initialSuggestion()` —
 * the concrete condition the user would commit if they accepted the
 * suggestion as-is, with no further editing.
 */
internal class CornerstoneDiscriminationScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    override fun score(s: SuggestedCondition): Int {
        if (ctx.cornerstones.isEmpty()) return 0
        val candidate = s.initialSuggestion()
        return ctx.cornerstones.count { !candidate.holds(it) }
    }
}
