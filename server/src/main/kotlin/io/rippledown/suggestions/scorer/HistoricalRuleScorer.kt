package io.rippledown.suggestions.scorer

import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.suggestions.SuggestionContext

/**
 * Scores a [SuggestedCondition] by the number of historical rules in the
 * rule tree whose conclusion matches the current action's target conclusion
 * and whose own conditions include one that is [sameAs] the candidate.
 *
 * This is the strongest signal available in Phase 1: the KB has literally
 * used this condition to justify the same comment in the past.
 *
 * When the context has no action, score is 0 for every candidate.
 *
 * Matching is by `Conclusion.id` — not reference identity — to survive
 * KB reloads. Candidate matching uses `Condition.sameAs`, which ignores
 * condition id, display text and user expression.
 */
internal class HistoricalRuleScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    private val historicalConditions: List<List<io.rippledown.model.condition.Condition>> =
        computeHistoricalConditions()

    override fun score(s: SuggestedCondition): Int {
        if (historicalConditions.isEmpty()) return 0
        val candidate = s.initialSuggestion()
        return historicalConditions.count { ruleConditions ->
            ruleConditions.any { it.sameAs(candidate) }
        }
    }

    /**
     * Returns, for every rule in the tree whose conclusion id matches the
     * action's target conclusion id, that rule's own conditions (not the
     * conditions inherited along the path — those are Phase 3 territory).
     */
    private fun computeHistoricalConditions(): List<List<io.rippledown.model.condition.Condition>> {
        val targetConclusionId = ctx.action?.targetConclusionId() ?: return emptyList()
        return ctx.ruleTree.rulesMatching { rule ->
            rule.conclusion?.id == targetConclusionId
        }.map { it.conditions.toList() }
    }
}

/**
 * The conclusion id this action is introducing (or whose conditions it is
 * reusing). Add / Replace both resolve to the comment being *added* to the
 * case; Remove resolves to the comment being removed so we surface the
 * conditions that previously gated it in.
 */
private fun io.rippledown.model.rule.RuleTreeChange.targetConclusionId(): Int? = when (this) {
    is ChangeTreeToAddConclusion -> toBeAdded.id
    is ChangeTreeToReplaceConclusion -> replacement.id
    is ChangeTreeToRemoveConclusion -> toBeRemoved.id
    else -> null
}
