package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class RDRCase(val name: String) {
    val caseData: MutableMap<String,String> = mutableMapOf()

    fun addValue(test: String, value: String) {
        caseData[test] = value
    }
}