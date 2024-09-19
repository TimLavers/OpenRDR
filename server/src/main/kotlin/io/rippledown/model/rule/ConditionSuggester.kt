package io.rippledown.model.rule

import io.rippledown.model.*
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.series.Trend
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.condition.structural.IsSingleEpisodeCase

typealias SuggestionFunction = (Attribute, TestResult?) -> SuggestedCondition?

class ConditionSuggester(
    attributes: Set<Attribute>,
    private val sessionCase: RDRCase
) {
    private val attributesInCase = sessionCase.attributes
    private val attributesNotInCase = attributes - attributesInCase

    fun suggestions(): List<SuggestedCondition> {
        return (caseStructureSuggestions() + episodicConditionSuggestions() + seriesConditionSuggestions()).toList()
            .sortedWith(Sorter())
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

    private fun caseStructureSuggestions() =
        attributeInCaseConditions() + attributeNotInCaseConditions() + episodeCountConditions()

    private fun episodeCountConditions(): List<SuggestedCondition> {
        return listOf(NonEditableSuggestedCondition(CaseStructureCondition(IsSingleEpisodeCase)))
    }

    private fun attributeInCaseConditions() = attributesInCase
        .map { isInCase(it) }
        .map { NonEditableSuggestedCondition(it) }
        .toSet()

    private fun attributeNotInCaseConditions() = attributesNotInCase
        .map { isNotInCase(it) }
        .map { NonEditableSuggestedCondition(it) }
        .toSet()

    private fun isInCase(attribute: Attribute) = CaseStructureCondition(null, IsPresentInCase(attribute))

    private fun isNotInCase(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute))

    private fun episodicFactories(): List<SuggestionFunction> {
        val signaturesToUse = mutableListOf<Signature>(Current)
        if (sessionCase.numberOfEpisodes() > 2) {
            signaturesToUse.add(AtMost(3))
            signaturesToUse.add(AtLeast(3))
        }
        if (sessionCase.numberOfEpisodes() > 1) {
            signaturesToUse.add(All)
            signaturesToUse.add(AtMost(1))
            signaturesToUse.add(AtLeast(1))
            signaturesToUse.add(AtMost(2))
            signaturesToUse.add(AtLeast(2))
            signaturesToUse.add(No)
        }
        return signaturesToUse.flatMap { episodicFactoriesForSignature(it) }
    }

    private fun episodicFactoriesForSignature(signature: Signature): List<SuggestionFunction> {
        return listOf(
            IsSuggestion(signature),
            NonEditableConditionSuggester(IsNumeric, signature),
            NonEditableConditionSuggester(IsNotNumeric, signature),
            RangeConditionSuggester(Low, signature),
            RangeConditionSuggester(Normal, signature),
            RangeConditionSuggester(High, signature),
            ExtendedLowRangeSuggestion(signature),
            ExtendedLowNormalRangeSuggestion(signature),
            ExtendedHighNormalRangeSuggestion(signature),
            ExtendedHighRangeSuggestion(signature),
            GreaterThanOrEqualsSuggestion(signature),
            LessThanOrEqualsSuggestion(signature),
            ContainsSuggestion(signature),
            DoesNotContainSuggestion(signature),
        )
    }
}

private fun trendFactories(): List<SuggestionFunction> {
    return listOf(
        TrendSuggestion(Increasing),
        TrendSuggestion(Decreasing)
    )
}

class Sorter : Comparator<SuggestedCondition> {
    override fun compare(o1: SuggestedCondition?, o2: SuggestedCondition?): Int {
        return o1!!.initialSuggestion().asText().compareTo(o2!!.initialSuggestion().asText())
    }
}

fun editableReal(testResult: TestResult?): EditableValue? {
    val cutoff = testResult?.value?.real
    return if (cutoff == null) null else EditableValue(testResult.value.text, Type.Real)
}

class IsNumericSuggestion(private val signature: Signature = Current) : SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult?.value?.real == null) null else NonEditableSuggestedCondition(
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
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val editableValue = editableReal(testResult) ?: return null
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
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        if (testResult == null) return null
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
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return EditableSuggestedCondition(EditableContainsCondition(attribute, value, signature))
    }
}

class DoesNotContainSuggestion(private val signature: Signature) : SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult == null) null else EditableSuggestedCondition(
            EditableDoesNotContainCondition(
                attribute,
                signature
            )
        )
    }
}

class IsSuggestion(private val signature: Signature) : SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, Is(value), signature))
    }
}

class NonEditableConditionSuggester(private val predicate: TestResultPredicate, private val signature: Signature) :
    SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult != null) NonEditableSuggestedCondition(
            EpisodicCondition(
                attribute,
                predicate,
                signature
            )
        ) else null
    }
}
class RangeConditionSuggester(private val predicate: TestResultPredicate, private val signature: Signature) :
    SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        if (testResult == null) return null
        val filter = EpisodicCondition(attribute, HighOrNormalOrLow, AtLeast(1))
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, predicate, signature), filter)
    }
}

class TrendSuggestion(private val trend: Trend) : SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        testResult?.value?.real ?: return null
        return NonEditableSuggestedCondition(SeriesCondition(null, attribute, trend))
    }
}
