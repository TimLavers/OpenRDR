package io.rippledown.model

class RDRCase(val name: String) {
    val caseData: MutableMap<String,String> = mutableMapOf()

    fun addValue(test: String, value: String) {
        caseData[test] = value
    }
}