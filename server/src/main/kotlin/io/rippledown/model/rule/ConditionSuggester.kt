package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.*
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberFunctions

class ConditionSuggester(private val attributes: Set<Attribute>,
                         private val sessionCase: RDRCase) {
    private val attributesInCase = sessionCase.attributes
    private val attributesNotInCase = attributes - attributesInCase

    fun suggestions(): List<Condition> {
        return (caseStructureSuggestions() + episodicConditionSuggestions()).toList().sortedWith(Sorter())
    }

    private fun episodicConditionSuggestions(): Set<Condition> {
        val predicateClasses = TestResultPredicate::class.sealedSubclasses
        val result = mutableSetOf<Condition>()
        attributesInCase.forEach {
            val currentValue = sessionCase.latestValue(it)
            if (currentValue != null) {
                result.add(EpisodicCondition(it, Is(currentValue), Current))
            }
        }
        return result
    }

    fun predicates(testResult: TestResult?): List<TestResultPredicate> {
        return factories(testResult).mapNotNull { it.createFor() }
//        predicateClass
//        println(mams)
//        predicateClass.co
    }

    private fun caseStructureSuggestions() = attributeInCaseConditions() + attributeNotInCaseConditions()

    private fun attributeInCaseConditions() = attributesInCase.map { presentAttributeCondition(it) }.toSet()
    private fun attributeNotInCaseConditions() = attributesNotInCase.map { absentAttributeCondition(it) }.toSet()

    private fun presentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsPresentInCase(attribute))
    private fun absentAttributeCondition(attribute: Attribute) = CaseStructureCondition(null, IsAbsentFromCase(attribute))
}
class Sorter: Comparator<Condition> {
    override fun compare(o1: Condition?, o2: Condition?): Int {
        return o1!!.asText().compareTo(o2!!.asText())
    }
}
sealed class PredicateFactory {
    abstract val testResult: TestResult?
    abstract fun createFor(): TestResultPredicate?
    fun stringValue() = if (testResult?.value == null) null else testResult!!.value.text
    fun doubleValue() = if (testResult?.value?.realReal == null) null else testResult!!.value.text.toDoubleOrNull()
}

data class GTEFactory(override val testResult: TestResult?) : PredicateFactory() {
    override fun createFor(): GreaterThanOrEquals? {
        val cutoff = doubleValue()
        return if (cutoff == null) null else GreaterThanOrEquals(cutoff)
    }
}
data class LTEFactory(override val testResult: TestResult?): PredicateFactory() {
    override fun createFor(): LessThanOrEquals? {
        val cutoff = doubleValue()
        return if (cutoff == null) null else LessThanOrEquals(cutoff)
    }
}
data class IsFactory(override val testResult: TestResult?): PredicateFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (stringValue() == null) null else Is(stringValue()!!)
    }
}
data class ContainsFactory(override val testResult: TestResult?): PredicateFactory() {
    override fun createFor(): TestResultPredicate? {
        return if (stringValue() == null) null else Contains(stringValue()!!)
    }
}
fun factories(testResult: TestResult?): List<PredicateFactory> {
    return listOf(GTEFactory(testResult), LTEFactory(testResult), IsFactory(testResult), ContainsFactory(testResult))
}
