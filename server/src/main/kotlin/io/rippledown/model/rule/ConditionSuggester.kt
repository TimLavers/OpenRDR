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
        return (caseStructureSuggestions() + episodicConditionSuggestions() + seriesConditionSuggestions()).toList().sortedWith(Sorter())
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

    private fun caseStructureSuggestions() = attributeInCaseConditions()+ attributeNotInCaseConditions() + episodeCountConditions()

    private fun episodeCountConditions(): List<SuggestedCondition> {
        return listOf(NonEditableSuggestedCondition(CaseStructureCondition(IsSingleEpisodeCase)))
    }

    private fun attributeInCaseConditions() = attributesInCase
        .map { presentAttributeCondition(it) }
        .map { NonEditableSuggestedCondition(it) }
        .toSet()

    private fun attributeNotInCaseConditions() = attributesNotInCase
        .map { absentAttributeCondition(it) }
        .map { NonEditableSuggestedCondition(it) }
        .toSet()

    private fun presentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsPresentInCase(attribute))

    private fun absentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute))

    private fun episodicFactories(): List<SuggestionFunction> {
        val signaturesToUse = mutableListOf<Signature>(Current)
        if (sessionCase.numberOfEpisodes() > 1) {
            signaturesToUse.add(All)
            for (i in 1..3) {
                signaturesToUse.add(AtMost(i))
                signaturesToUse.add(AtLeast(i))
            }
            signaturesToUse.add(No)
        }
        return signaturesToUse.flatMap { episodicFactoriesForSignature(it) }
//        val result = mutableListOf<SuggestionFunction>()
//        signaturesToUse.forEach { result.addAll(episodicFactoriesForSignature(it))}
//        return result
    }

    private fun episodicFactoriesForSignature(signature: Signature): List<SuggestionFunction> {
        if (signature == Current) {
        return listOf(
            ExtendedLowRangeSuggestion,
            ExtendedLowNormalRangeSuggestion,
            ExtendedHighNormalRangeSuggestion,
            ExtendedHighRangeSuggestion,
        )} else {
            return listOf(
                IsSuggestion(signature),
                NonEditableConditionSuggester(IsNumeric, signature),
                NonEditableConditionSuggester(Low, signature),
                NonEditableConditionSuggester(Normal, signature),
                NonEditableConditionSuggester(High, signature),
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
class IsNumericSuggestion(private val signature: Signature = Current): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult?.value?.real == null) null else NonEditableSuggestedCondition(EpisodicCondition(attribute, IsNumeric, signature))
    }
}
abstract class CutoffSuggestion(val signature: Signature): SuggestionFunction {
    abstract fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val editableValue = editableReal(testResult) ?: return null
        return EditableSuggestedCondition(createEditableCondition(attribute, editableValue))
    }
}
class GreaterThanOrEqualsSuggestion(signature: Signature): CutoffSuggestion(signature) {
    override fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition {
        return EditableGreaterThanEqualsCondition(attribute, editableValue, signature)
    }
}
class LessThanOrEqualsSuggestion(signature: Signature): CutoffSuggestion(signature) {
    override fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition {
        return EditableLTECondition(attribute, editableValue)
    }
}
abstract class ExtendedRangeSuggestion: SuggestionFunction {
    abstract fun createEditableCondition(attribute: Attribute): EditableCondition
    abstract fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value): Boolean
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val referenceRange = testResult?.referenceRange ?: return null
        return if (rangeAndValueSuitable(referenceRange, testResult.value)) EditableSuggestedCondition(createEditableCondition(attribute)) else null
    }
}
object ExtendedLowRangeSuggestion: ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) = EditableExtendedLowRangeCondition(attribute)
    override fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value) = referenceRange.isLow(value)
}
object ExtendedLowNormalRangeSuggestion: ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) = EditableExtendedLowNormalRangeCondition(attribute)
    override fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value) = referenceRange.isLow(value) || referenceRange.isNormal(value)
}
object ExtendedHighNormalRangeSuggestion: ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) = EditableExtendedHighNormalRangeCondition(attribute)
    override fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value) = referenceRange.isHigh(value) || referenceRange.isNormal(value)
}
object ExtendedHighRangeSuggestion: ExtendedRangeSuggestion() {
    override fun createEditableCondition(attribute: Attribute) = EditableExtendedHighRangeCondition(attribute)
    override fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value) = referenceRange.isHigh(value)
}

class ContainsSuggestion(private val signature: Signature): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return EditableSuggestedCondition(EditableContainsCondition(attribute, value, signature))
    }
}
class DoesNotContainSuggestion(private val signature: Signature): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult == null) null else EditableSuggestedCondition(EditableDoesNotContainCondition(attribute, signature))
    }
}
class IsSuggestion(private val signature: Signature): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return NonEditableSuggestedCondition(EpisodicCondition(attribute, Is(value), signature))
    }
}
class NonEditableConditionSuggester(private val predicate: TestResultPredicate, private val signature: Signature): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (testResult != null) NonEditableSuggestedCondition(EpisodicCondition(attribute, predicate, signature)) else null
    }
}
fun hasRange(testResult: TestResult?): Boolean {
    return testResult?.referenceRange != null
}
class TrendSuggestion(private val trend: Trend): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        testResult?.value?.real ?: return null
        return NonEditableSuggestedCondition(SeriesCondition(null, attribute, trend))
    }
}
