package io.rippledown.model.rule

import io.rippledown.model.*
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase

typealias SuggestionFunction = (Attribute, TestResult?) -> SuggestedCondition?

class ConditionSuggester(
    attributes: Set<Attribute>,
    private val sessionCase: RDRCase
) {
    private val attributesInCase = sessionCase.attributes
    private val attributesNotInCase = attributes - attributesInCase

    fun suggestions(): List<SuggestedCondition> {
        return (caseStructureSuggestions() + episodicConditionSuggestions()).toList().sortedWith(Sorter())
    }

    private fun episodicConditionSuggestions(): Set<SuggestedCondition> {
        val firstCut = mutableSetOf<SuggestedCondition>()
        attributesInCase.forEach { attribute ->
            val currentValue = sessionCase.getLatest(attribute)
            suggestionFactories().forEach {
                val suggestedCondition = it(attribute, currentValue)
                if (suggestedCondition != null) {
                    firstCut.add(suggestedCondition)
                }
            }
        }
        return firstCut.filter { it.shouldBeSuggestedForCase(sessionCase) }.toSet()
    }

    private fun caseStructureSuggestions() = attributeInCaseConditions() + attributeNotInCaseConditions()

    private fun attributeInCaseConditions() = attributesInCase
        .map { presentAttributeCondition(it) }
        .map { FixedSuggestedCondition(it) }
        .toSet()

    private fun attributeNotInCaseConditions() = attributesNotInCase
        .map { absentAttributeCondition(it) }
        .map { FixedSuggestedCondition(it) }
        .toSet()

    private fun presentAttributeCondition(attribute: Attribute) =
        CaseStructureCondition(null, IsPresentInCase(attribute))

    private fun absentAttributeCondition(attribute: Attribute) =
        CaseStructureCondition(null, IsAbsentFromCase(attribute))
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
abstract class CutoffSuggestion: SuggestionFunction {
    abstract fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val editableValue = editableReal(testResult) ?: return null
        return EditableSuggestedCondition(createEditableCondition(attribute, editableValue))
    }
}
object GreaterThanOrEqualsSuggestion: CutoffSuggestion() {
    override fun createEditableCondition(attribute: Attribute, editableValue: EditableValue): EditableCondition {
        return EditableGTECondition(attribute, editableValue)
    }
}
object LessThanOrEqualsSuggestion: CutoffSuggestion(){
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
    override fun createEditableCondition(attribute: Attribute): EditableCondition {
        return EditableExtendedLowRangeCondition(attribute)
    }

    override fun rangeAndValueSuitable(referenceRange: ReferenceRange, value: Value): Boolean {
        return referenceRange.isLow(value)
    }
}

object ContainsSuggestion: SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return EditableSuggestedCondition(EditableContainsCondition(attribute, value))
    }
}
object IsSuggestion: SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        val value = testResult?.value?.text ?: return null
        return FixedSuggestedCondition(EpisodicCondition(attribute, Is(value), Current))
    }
}
class RangeConditionSuggester(private val predicate: TestResultPredicate): SuggestionFunction {
    override fun invoke(attribute: Attribute, testResult: TestResult?): SuggestedCondition? {
        return if (hasRange(testResult)) FixedSuggestedCondition(EpisodicCondition(attribute, predicate, Current)) else null
    }
}
fun suggestionFactories(): List<SuggestionFunction> {
    return listOf(
            GreaterThanOrEqualsSuggestion,
            LessThanOrEqualsSuggestion,
            ContainsSuggestion,
            IsSuggestion,
            RangeConditionSuggester(Low),
            RangeConditionSuggester(Normal),
            RangeConditionSuggester(High),
            ExtendedLowRangeSuggestion
        )
}
fun hasRange(testResult: TestResult?): Boolean {
    return testResult?.referenceRange != null
}
