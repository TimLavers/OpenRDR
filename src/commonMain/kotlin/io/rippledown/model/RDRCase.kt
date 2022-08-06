package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class RDRCase(val name: String) {
    val caseData: MutableMap<String,MutableSet<TestResult>> = mutableMapOf()

    fun addValue(attribute: String, value: String, date: Long) {
        addResult(attribute, TestResult(value, date))
    }

    fun addResult(attribute: String, result: TestResult) {
        val valuesForAttribute = caseData[attribute] ?: mutableSetOf()
        valuesForAttribute.add(result)
        caseData[attribute] = valuesForAttribute
    }

    fun get(attribute: Attribute): Set<TestResult>? {
        return caseData[attribute.name]
    }
    
    fun getLatest(attribute: Attribute): TestResult? {
        val all = caseData[attribute.name] ?: return null
        return all.first() // todo fix this
    }
    
    fun latestEpisode(): Map<String, TestResult> {
        return caseData.mapValues { it.value.first() } // todo fix this
    }
}