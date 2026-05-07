package io.rippledown.suggestions.scorer

import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.EditableComparisonCondition
import io.rippledown.model.condition.edit.EditableGreaterThanEqualsCondition
import io.rippledown.model.condition.edit.EditableLessThanEqualsCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.predicate.TestResultPredicate
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.suggestions.SuggestionContext
import kotlin.reflect.KClass

/**
 * Scores a [SuggestedCondition] by the number of historical rules in the
 * rule tree whose conclusion matches the current action's target conclusion
 * and whose own conditions include one that matches the candidate.
 *
 * This is the strongest signal available in Phase 1: the KB has literally
 * used this condition to justify the same comment in the past.
 *
 * When the context has no action, score is 0 for every candidate.
 *
 * Matching is by `Conclusion.id` — not reference identity — to survive
 * KB reloads.
 *
 * Candidate matching is normally by [Condition.sameAs], which ignores
 * condition id, display text and user expression but requires predicate
 * equality. For *editable* numeric-threshold candidates
 * ([EditableGreaterThanEqualsCondition] / [EditableLessThanEqualsCondition])
 * exact predicate equality would force the historical rule to use the same
 * threshold value as the current case — almost never true, and not what
 * the historical signal is trying to capture. For those candidates
 * matching is family-wise: same attribute, same signature, same
 * comparison direction (`≥` / `≤`), regardless of the cutoff value.
 *
 * Example: a historical rule "eGFR ≥ 70 → Elevated Hb may be significant."
 * matches the editable `eGFR ≥ <case value>` candidate offered when the
 * user adds the same comment to a new case whose eGFR happens to be 74.
 */
internal class HistoricalRuleScorer(
    private val ctx: SuggestionContext,
) : SuggestionScorer {

    private val historicalConditions: List<List<Condition>> =
        computeHistoricalConditions()

    override fun score(s: SuggestedCondition): Int {
        if (historicalConditions.isEmpty()) return 0
        val matches = historicalMatcherFor(s)
        return historicalConditions.count { ruleConditions ->
            ruleConditions.any(matches)
        }
    }

    /**
     * Returns, for every rule in the tree whose conclusion id matches the
     * action's target conclusion id, that rule's own conditions (not the
     * conditions inherited along the path — those are Phase 3 territory).
     */
    private fun computeHistoricalConditions(): List<List<Condition>> {
        val targetConclusionId = ctx.action?.targetConclusionId() ?: return emptyList()
        return ctx.ruleTree.rulesMatching { rule ->
            rule.conclusion?.id == targetConclusionId
        }.map { it.conditions.toList() }
    }
}

/**
 * Builds the predicate used to test whether a historical condition counts
 * as a match for the candidate suggestion.
 *
 * For editable comparison candidates (`≥` / `≤`) the match is family-wise
 * (same attribute, same signature, same comparison direction). For every
 * other candidate it falls back to [Condition.sameAs] on the candidate's
 * `initialSuggestion()`, preserving the historical exact-match semantics
 * for `is high`, `is normal`, equality, contains, trends, etc.
 */
private fun historicalMatcherFor(s: SuggestedCondition): (Condition) -> Boolean {
    val editable = s.editableCondition()
    if (editable is EditableComparisonCondition) {
        val predicateClass = comparisonPredicateClass(editable)
        val attribute = editable.attribute
        val signature = editable.signature
        return { historical ->
            historical is EpisodicCondition &&
                    historical.attribute.isEquivalent(attribute) &&
                    historical.signature == signature &&
                    predicateClass.isInstance(historical.predicate)
        }
    }
    val candidate = s.initialSuggestion()
    return { it.sameAs(candidate) }
}

private fun comparisonPredicateClass(
    editable: EditableComparisonCondition,
): KClass<out TestResultPredicate> = when (editable) {
    is EditableGreaterThanEqualsCondition -> GreaterThanOrEquals::class
    is EditableLessThanEqualsCondition -> LessThanOrEquals::class
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
