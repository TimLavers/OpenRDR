package io.rippledown.suggestions.scorer

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.suggestions.SuggestionContext

/**
 * Scores a [SuggestedCondition] by whether the candidate's attribute has
 * a current value that is **out of reference range** (low or high) in
 * the session case.
 *
 * Score: `1` if the latest value for the attribute is flagged low or
 * high, `0` for normal / no reference range / no value.
 *
 * The intent is purely a tiebreak: when nothing else has separated
 * candidates (no historical signal, no comment overlap, no cornerstone
 * discrimination), an out-of-range attribute is a more interesting
 * subject for the rule than an in-range one. For the case in image 3,
 * AST `36*` (high) ranks above ALT `29` (normal) at this stage even
 * though they are alphabetically reversed.
 *
 * Conditions without a meaningful attribute (e.g.
 * [io.rippledown.model.condition.structural.IsSingleEpisodeCase]) score
 * 0 and fall through to the alphabetic tiebreak.
 */
internal class OutOfRangeScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    /**
     * Cache the answer per attribute: a single case lookup per attribute
     * is enough to cover every candidate referencing it.
     */
    private val outOfRangeByAttribute: Map<Attribute, Boolean> = ctx.sessionCase.attributes
        .associateWith { attribute -> isOutOfRange(ctx.sessionCase, attribute) }

    override fun score(s: SuggestedCondition): Int {
        val attribute = attributeOf(s.initialSuggestion()) ?: return 0
        return if (outOfRangeByAttribute[attribute] == true) 1 else 0
    }

    private fun isOutOfRange(case: RDRCase, attribute: Attribute): Boolean {
        val result = case.getLatest(attribute) ?: return false
        return result.isLow() || result.isHigh()
    }

    private fun attributeOf(condition: Condition): Attribute? = when (condition) {
        is EpisodicCondition -> condition.attribute
        is SeriesCondition -> condition.attribute
        is CaseStructureCondition -> when (val p = condition.predicate) {
            is IsPresentInCase -> p.attribute
            is IsAbsentFromCase -> p.attribute
            else -> null
        }

        else -> null
    }
}
