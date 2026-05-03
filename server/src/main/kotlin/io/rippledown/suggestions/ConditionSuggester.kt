package io.rippledown.suggestions

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.Result
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.series.Trend
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

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
        return RelevanceRanker(ctx).rank(pruneSubsumed(generated))
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
        if (signature == Current) {
            factories += ContainsSuggestion(signature)
            factories += DoesNotContainSuggestion(signature)
        }
        return factories
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

class ContainsSuggestion(private val signature: Signature) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
        val value = Result?.value?.text ?: return null
        // "contains" only makes clinical sense for free-text values.
        // For numeric values (e.g. HAEMOGLOBIN 194) it produces noise
        // like `contains "194"` that crowds the top-20.
        if (Result.value.real != null) return null
        if (value.isBlank()) return null
        return EditableSuggestedCondition(EditableContainsCondition(attribute, value, signature))
    }
}

class DoesNotContainSuggestion(private val signature: Signature) : SuggestionFunction {
    override fun invoke(attribute: Attribute, Result: Result?): SuggestedCondition? {
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
