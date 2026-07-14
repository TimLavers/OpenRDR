package io.rippledown.simpleapi

interface SimpleInterpreter {
    fun interpretStringMap(caseData: Map<String, String>): String
}