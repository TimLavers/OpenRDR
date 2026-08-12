package io.rippledown.suggestions.scorer

import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.rule.ChangeTreeToAddAssignment
import io.rippledown.model.rule.ChangeTreeToRemoveAssignment
import io.rippledown.model.rule.ChangeTreeToReplaceAssignment
import io.rippledown.model.rule.RuleTreeChange
import io.rippledown.suggestions.SuggestionContext

/**
 * Scores a [SuggestedCondition] by the number of historical rules in the
 * rule tree that assign the current action's target attribute and whose own
 * conditions include one that `sameAs` the candidate's
 * `initialSuggestion()`.
 *
 * This is the strongest signal available in Phase 1: the KB has literally
 * used this exact condition to justify the same comment in the past.
 *
 * When the context has no action, score is 0 for every candidate.
 *
 * Matching is by attribute id — not reference identity — to survive KB
 * reloads, and condition matching is by [Condition.sameAs] (ignores
 * condition id, display text and user expression but requires predicate
 * equality, including the cutoff value of `≥` / `≤` predicates).
 *
 * Note that the *literal* historical conditions are injected as candidates
 * in their own right by `ConditionSuggester` (filtered to those that hold
 * on the session case). That injection, combined with strict `sameAs`
 * matching here, surfaces clinically meaningful cutoffs (`eGFR ≥ 70`)
 * verbatim and ranks them at the top — instead of relaxing this matcher
 * to family-wise across cutoff values, which would also boost the
 * case-pinned editable candidate (`eGFR ≥ 74`) on the same signal.
 */
internal class HistoricalRuleScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    private val historicalConditions: List<List<Condition>> =
        computeHistoricalConditions()

    override fun score(s: SuggestedCondition): Int {
        if (historicalConditions.isEmpty()) return 0
        val candidate = s.initialSuggestion()
        return historicalConditions.count { ruleConditions ->
            ruleConditions.any { it.sameAs(candidate) }
        }
    }

    /**
     * Returns, for every rule in the tree that assigns the action's target
     * attribute, that rule's own conditions (not the conditions inherited
     * along the path — those are Phase 3 territory).
     */
    private fun computeHistoricalConditions(): List<List<Condition>> {
        val targetAttrId = ctx.action?.targetAttributeId() ?: return emptyList()
        return ctx.ruleTree.rulesMatching { rule ->
            rule.assignment?.attribute?.id == targetAttrId
        }.map { it.conditions.toList() }
    }
}

/**
 * The attribute id this action is targeting. For Add it is the attribute
 * being assigned; for Replace it is the *replacement* attribute (the new
 * comment); for Remove it is the attribute being retracted.
 */
internal fun RuleTreeChange.targetAttributeId(): Int? = when (this) {
    is ChangeTreeToAddAssignment -> toBeAdded.attribute.id
    is ChangeTreeToReplaceAssignment -> replacement.attribute.id
    is ChangeTreeToRemoveAssignment -> toBeRemoved.attribute.id
    else -> null
}
