package io.rippledown.suggestions

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.Result
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.series.Trend
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import io.rippledown.suggestions.scorer.targetConclusionId

typealias SuggestionFunction = (Attribute, Result?) -> SuggestedCondition?

class ConditionSuggester(private val ctx: SuggestionContext) {
    private val sessionCase: RDRCase = ctx.sessionCase
    private val attributesInCase = sessionCase.attributes

    fun suggestions(): List<SuggestedCondition> = allSuggestions().take(MAX_SUGGESTIONS)

    /**
     * All generated suggestions in ranking order, *without* the [MAX_SUGGESTIONS] cap.
     * Exposed for unit tests that assert on the full generator output; production
     * call sites should always go through [suggestions].
     */
    internal fun allSuggestions(): List<SuggestedCondition> {
        val generated = caseStructureSuggestions() + episodicConditionSuggestions() + seriesConditionSuggestions()
        val withHistorical = generated + historicalConditionSuggestions(generated)
        return RelevanceRanker(ctx).rank(pruneSubsumed(withHistorical))
    }

    /**
     * Injects, as candidates in their own right, the literal conditions of every
     * rule in [SuggestionContext.ruleTree] whose conclusion id matches the
     * action's target conclusion id, restricted to those that hold on the session
     * case. See "Historical-condition injection" in
     * `documentation/design/targeted_suggested_conditions_phase_1.md` for the
     * rationale: pathology cutoffs in existing rules are usually the clinically
     * defensible ones (evidence-based guideline thresholds, departmental
     * conventions), so surfacing the literal `eGFR ≥ 70` alongside the
     * case-pinned editable `eGFR ≥ 74` lets the user pick the clinical cutoff
     * with one click while [HistoricalRuleScorer] boosts it via plain `sameAs`.
     *
     * Dedup is by `sameAs` against [alreadyGenerated]: a historical condition
     * that some existing candidate already covers is dropped, so we never offer
     * two structurally identical entries. The dedup keeps the editable form when
     * cutoffs coincide, since editables remain user-adjustable.
     */
    private fun historicalConditionSuggestions(
        alreadyGenerated: Collection<SuggestedCondition>,
    ): List<SuggestedCondition> {
        val targetConclusionId = ctx.action?.targetConclusionId() ?: return emptyList()
        val existing = alreadyGenerated.map { it.initialSuggestion() }
        val seen = mutableListOf<Condition>()
        val results = mutableListOf<SuggestedCondition>()
        ctx.ruleTree.rulesMatching { rule ->
            rule.conclusion?.id == targetConclusionId
        }.forEach { rule ->
            rule.conditions.forEach { condition ->
                if (!condition.holds(sessionCase)) return@forEach
                if (existing.any { it.sameAs(condition) }) return@forEach
                if (seen.any { it.sameAs(condition) }) return@forEach
                seen.add(condition)
                results.add(NonEditableSuggestedCondition(condition))
            }
        }
        return results
    }

    /**
     * Drops candidates that are strictly implied by other candidates in
     * the same set. The only subsumption rule modelled today:
     *
     *   `all <attr> are <X>` (signature `All`, range predicate Low / Normal
     *   / High) implies `no <attr> is <Y>` for every other range predicate
     *   Y, since the three range predicates are mutually exclusive.
     *
     * Without this, a HAEMOGLOBIN-is-high case yields `all HAEMOGLOBIN are
     * high`, `no HAEMOGLOBIN is low` and `no HAEMOGLOBIN is normal` side
     * by side — the last two are tautological consequences of the first
     * and add no information for the user.
     */
    private fun pruneSubsumed(candidates: Collection<SuggestedCondition>): Collection<SuggestedCondition> {
        val rangePredicates = setOf(Low, Normal, High)
        val attributesAllOfRange: Set<Pair<Attribute, TestResultPredicate>> = candidates
            .mapNotNull { c ->
                val cond = c.initialSuggestion() as? EpisodicCondition ?: return@mapNotNull null
                if (cond.signature == All && cond.predicate in rangePredicates) {
                    cond.attribute to cond.predicate
                } else null
            }.toSet()

        if (attributesAllOfRange.isEmpty()) return candidates
        return candidates.filterNot { c ->
            val cond = c.initialSuggestion() as? EpisodicCondition ?: return@filterNot false
            if (cond.signature != No || cond.predicate !in rangePredicates) return@filterNot false
            attributesAllOfRange.any { (attr, allPred) ->
                attr == cond.attribute && allPred != cond.predicate
            }
        }
    }

    private fun episodicConditionSuggestions() = createSuggestions(episodicFactories())

    private fun seriesConditionSuggestions() = createSuggestions(trendFactories())

    private fun createSuggestions(factories: List<SuggestionFunction>): Set<SuggestedCondition> {
        val firstCut = mutableSetOf<SuggestedCondition>()
        attributesInCase.forEach { attribute ->
            val currentValue = sessionCase.getLatest(attribute)
            factories.forEach {
                val suggestedCondition = it(attribute, currentValue)
                if (suggestedCondition != null) {
                    firstCut.add(suggestedCondition)
                }
            }
        }
        return firstCut.filter { it.shouldBeSuggestedForCase(sessionCase) }.toSet()
    }

    private fun caseStructureSuggestions() = episodeCountConditions()

    private fun episodeCountConditions(): List<SuggestedCondition> {
        return if (sessionCase.numberOfEpisodes() == 1) {
            listOf(NonEditableSuggestedCondition(CaseStructureCondition(IsSingleEpisodeCase)))
        } else emptyList()
    }

    private fun episodicFactories(): List<SuggestionFunction> {
        // Signature vocabulary kept deliberately small: "current" (now),
        // "all" (holds across every episode) and "no" (holds for none).
        // The AtLeast(n) / AtMost(n) shapes were judged to add more noise
        // than value and were dropped after the Phase 1 review.
        val signaturesToUse = mutableListOf<Signature>(Current)
        if (sessionCase.numberOfEpisodes() > 1) {
            signaturesToUse.add(All)
            signaturesToUse.add(No)
        }
        return signaturesToUse.flatMap { episodicFactoriesForSignature(it) }
    }

    private fun episodicFactoriesForSignature(signature: Signature): List<SuggestionFunction> {
        // Value-predicate shapes kept for every signature (Current, All,
        // AtLeast/AtMost, No). The previously-generated IsNumeric /
        // IsNotNumeric and ExtendedRange ("by at most N%") variants were
        // judged to add no clinical value and were removed to stop them
        // crowding the top-20.
        val factories = mutableListOf<SuggestionFunction>(
            IsSuggestion(signature),
            RangeConditionSuggester(Low, signature),
            RangeConditionSuggester(Normal, signature),
            RangeConditionSuggester(High, signature),
            GreaterThanOrEqualsSuggestion(signature),
            LessThanOrEqualsSuggestion(signature),
        )
        // Text-predicate shapes (Contains / DoesNotContain) only make sense
        // for the current episode of a non-numeric value. "All", "at least
        // 1", etc. variants of Contains on a numeric value are useless.
        // Furthermore, restrict them to attributes the user has *already*
        // matched on with a substring predicate in some existing rule. Free
        // text fields (e.g. addresses, comments) otherwise produce noise like
        // `Address contains "Unionstrasse 4, ..."` and `Address does not
        // contain ""` that crowd the top-20 with values the user never asked
        // to substring-match.
        if (signature == Current) {
            val allowed = attributesUsedInSubstringRules()
            factories += ContainsSuggestion(signature, allowed)
            factories += DoesNotContainSuggestion(signature, allowed)
        }
        return factories
    }

    /**
     * Attributes that appear in some existing rule with a [Contains] or
     * [DoesNotContain] predicate. Only these attributes are eligible for
     * substring-style suggestions; see [episodicFactoriesForSignature].
     */
    private fun attributesUsedInSubstringRules(): Set<Attribute> {
        val result = mutableSetOf<Attribute>()
        ctx.ruleTree.rules().forEach { rule ->
            rule.conditions.forEach { condition ->
                if (condition is EpisodicCondition &&
                    (condition.predicate is Contains || condition.predicate is DoesNotContain)
                ) {
                    result.add(condition.attribute)
                }
            }
        }
        return result
    }
}

private fun trendFactories(): List<SuggestionFunction> {
    return listOf(
        TrendSuggestion(Increasing),
        TrendSuggestion(Decreasing)
    )
}

fun editableReal(Result: Result?): EditableValue? {
    val cutoff = Result?.value?.real
    return if (cutoff == null) null else EditableValue(Result.value.text, Type.Real)
}

class IsNumericSuggestion(private val signature: Signature = Current) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        return if (Result?.value?.real == null) null else NonEditableSuggestedCondition(
            EpisodicCondition(
                attribute,
                IsNumeric,
                signature
            )
        )
    }
}

abstract class CutoffSuggestion(val signature: Signature) : SuggestionFunction {
    abstract fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        val editableValue = editableReal(Result) ?: return null
        return EditableSuggestedCondition(createEditableCondition(attribute, editableValue))
    }
}

class GreaterThanOrEqualsSuggestion(signature: Signature) : CutoffSuggestion(signature) {
    override fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition {
        return EditableGreaterThanEqualsCondition(attribute, editableValue, signature)
    }
}

class LessThanOrEqualsSuggestion(signature: Signature) : CutoffSuggestion(signature) {
    override fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition {
        return EditableLessThanEqualsCondition(attribute, editableValue, signature)
    }
}

abstract class ExtendedRangeSuggestion : SuggestionFunction {
    abstract fun createEditableCondition(attribute: Attribute): EditableCondition
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        if (Result == null) return null
        return EditableSuggestedCondition(createEditableCondition(attribute))
    }
}

class ExtendedLowRangeSuggestion(private val signature: Signature) : ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) = EditableExtendedLowRangeCondition(attribute, signature)
}

class ExtendedLowNormalRangeSuggestion(private val signature: Signature) : ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) =
        EditableExtendedLowNormalRangeCondition(attribute, signature)
}

class ExtendedHighNormalRangeSuggestion(private val signature: Signature) : ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) =
        EditableExtendedHighNormalRangeCondition(attribute, signature)
}

class ExtendedHighRangeSuggestion(private val signature: Signature) : ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) =
        EditableExtendedHighRangeCondition(attribute, signature)
}

/**
 * Substring-style suggestions are restricted to attributes the user has
 * already substring-matched on in some existing rule. [allowedAttributes] is
 * that allowlist; pass `null` (the default) to keep the suggestion factory
 * unconstrained, which is convenient for unit tests of the factory itself.
 * Production wiring goes through [ConditionSuggester], which always supplies
 * a non-null allowlist computed from the rule tree.
 */
class ContainsSuggestion(
    private val signature: Signature,
    private val allowedAttributes: Set<Attribute>? = null,
) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        if (allowedAttributes != null && attribute !in allowedAttributes) return null
        val value = Result?.value?.text ?: return null
        // "contains" only makes clinical sense for free-text values.
        // For numeric values (e.g. HAEMOGLOBIN 194) it produces noise
        // like `contains "194"` that crowds the top-20.
        if (Result.value.real != null) return null
        if (value.isBlank()) return null
        return EditableSuggestedCondition(EditableContainsCondition(attribute, value, signature))
    }
}

class DoesNotContainSuggestion(
    private val signature: Signature,
    private val allowedAttributes: Set<Attribute>? = null,
) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        if (allowedAttributes != null && attribute !in allowedAttributes) return null
        if (Result == null) return null
        // Mirror ContainsSuggestion: only offer for non-numeric,
        // non-empty text. `does not contain ""` is never useful.
        if (Result.value.real != null) return null
        if (Result.value.text.isBlank()) return null
        return EditableSuggestedCondition(EditableDoesNotContainCondition(attribute, signature))
    }
}

class IsSuggestion(private val signature: Signature) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        val value = Result?.value?.text ?: return null
        // `is "<numeric value>"` is almost never what the user wants:
        // the threshold is pinned to this case's exact reading and the
        // next case a hundredth of a unit away fails it. The
        // `≥ <editable>` / `≤ <editable>` candidates already cover the
        // numeric-threshold intent with a user-editable cutoff, and
        // `is high` / `is low` cover the symbolic intent. So for
        // numeric values the `Is` variant is redundant clutter. Keep it
        // for short coded text values (`Sex is "M"`, `Status is "stable"`).
        if (Result.value.real != null) return null
        if (value.isBlank()) return null
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, Is(value), signature))
    }
}

class NonEditableConditionSuggester(private val predicate: TestResultPredicate, private val signature: Signature) :
    SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        if (Result == null) return null
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, predicate, signature))
    }
}

class RangeConditionSuggester(private val predicate: TestResultPredicate, private val signature: Signature) :
    SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        if (Result == null) return null
        val filter = EpisodicCondition(attribute, HighOrNormalOrLow, AtLeast(1))
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, predicate, signature), filter)
    }
}

class TrendSuggestion(private val trend: Trend) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        Result?.value?.real ?: return null
        return NonEditableSuggestedCondition(SeriesCondition(null, attribute, trend))
    }
}
