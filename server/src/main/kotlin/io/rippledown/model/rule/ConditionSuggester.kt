package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase

class ConditionSuggester(private val attributes: Set<Attribute>,
                         private val sessionCase: RDRCase) {
    private val attributesInCase = sessionCase.attributes
    private val attributesNotInCase = attributes - attributesInCase

    fun suggestions(): List<Condition> {
        return caseStructureSuggestions().toList().sortedWith(Sorter())
    }

    private fun episodicConditionSuggestions() {

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