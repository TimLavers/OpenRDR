package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class RDRCase(val name: String) {
    val caseData: MutableMap<String,TestResult> = mutableMapOf()

    fun addValue(attribute: String, value: String) {
        caseData[attribute] = TestResult(value)
    }

    fun addResult(attribute: String, result: TestResult) {
        caseData[attribute] = result
    }

    fun addValue(attribute: Attribute, value: String) {
        caseData[attribute.name] = TestResult(value)
    }

    fun get(attribute: Attribute): TestResult? {
        return caseData[attribute.name]
    }
}