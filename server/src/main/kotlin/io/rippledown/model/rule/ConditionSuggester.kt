package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase

class ConditionSuggester(private val attributes: Set<Attribute>,
                         private val sessionCase: RDRCase) {
    private val attributesInCase = sessionCase.attributes
    private val attributesNotInCase = attributes - attributesInCase

    fun suggestions(): List<SuggestedCondition> {
        return (caseStructureSuggestions() + episodicConditionSuggestions()).toList().sortedWith(Sorter())
    }

    private fun episodicConditionSuggestions(): Set<SuggestedCondition> {
        val firstCut = mutableSetOf<SuggestedCondition>()
        attributesInCase.forEach {attribute ->
            val currentValue = sessionCase.getLatest(attribute)
            factories(currentValue).forEach {
                val suggestedCondition = it.suggestion(attribute)
                if (suggestedCondition != null) {
                    firstCut.add(suggestedCondition)
                }
            }
        }
        return firstCut.filter { it.initialSuggestion().holds(sessionCase) }.toSet()
    }

    fun predicates(testResult: TestResult?): List<TestResultPredicate> {
        return factories(testResult).mapNotNull { it.createFor() }
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

    private fun presentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsPresentInCase(attribute))
    private fun absentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute))
}
class Sorter: Comparator<SuggestedCondition> {
    override fun compare(o1: SuggestedCondition?, o2: SuggestedCondition?): Int {
        return o1!!.initialSuggestion().asText().compareTo(o2!!.initialSuggestion().asText())
    }
}
sealed class EpisodicSuggestedConditionFactory {
    abstract val testResult: TestResult?
    abstract fun createFor(): TestResultPredicate?
    open fun suggestion(attribute: Attribute): SuggestedCondition? = null
    fun stringValue() = if (testResult?.value == null) null else testResult!!.value.text
    fun doubleValue() = if (testResult?.value?.realReal == null) null else testResult!!.value.text.toDoubleOrNull()
}
data class GTEFactory(override val testResult: TestResult?) : EpisodicSuggestedConditionFactory() {
    override fun createFor(): GreaterThanOrEquals? {
        val cutoff = doubleValue()
        return if (cutoff == null) null else GreaterThanOrEquals(cutoff)
    }

    override fun suggestion(attribute: Attribute): SuggestedCondition? {
        val cutoff = doubleValue() ?: return null
        val editableCondition = EditableGTECondition(attribute, EditableValue(stringValue()!!, Type.Real),)
        return EditableSuggestedCondition(editableCondition.condition(stringValue()!!), editableCondition)
    }
}
data class LTEFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): LessThanOrEquals? {
        val cutoff = doubleValue()
        return if (cutoff == null) null else LessThanOrEquals(cutoff)
    }
}
data class IsFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (stringValue() == null) null else Is(stringValue()!!)
    }

    override fun suggestion(attribute: Attribute): SuggestedCondition? {
        val argument = stringValue() ?: return null
        val suggestion = EpisodicCondition(attribute, Is(argument), Current)
        return FixedSuggestedCondition(suggestion)
    }
}
data class ContainsFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (stringValue() == null) null else Contains(stringValue()!!)
    }
}
data class LowFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (testResult?.referenceRange == null) null else Low
    }
}
data class NormalFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (testResult?.referenceRange == null) null else Normal
    }
}
data class HighFactory(override val testResult: TestResult?): EpisodicSuggestedConditionFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (testResult?.referenceRange == null) null else High
    }
}
fun factories(testResult: TestResult?): List<EpisodicSuggestedConditionFactory> {
    return listOf(
        GTEFactory(testResult),
        LTEFactory(testResult),
        IsFactory(testResult),
        ContainsFactory(testResult),
        LowFactory(testResult),
        NormalFactory(testResult),
        HighFactory(testResult)
    )
}
