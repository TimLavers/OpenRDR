package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class RDRCase(val name: String) {
    val caseData: MutableMap<String,Result> = mutableMapOf()

    fun addValue(attribute: String, value: String) {
        caseData[attribute] = Result(value)
    }

    fun addResult(attribute: String, result: Result) {
        caseData[attribute] = result
    }

    fun addValue(attribute: Attribute, value: String) {
        caseData[attribute.name] = Result(value)
    }

    fun get(attribute: Attribute): Result? {
        return caseData[attribute.name]
    }
}