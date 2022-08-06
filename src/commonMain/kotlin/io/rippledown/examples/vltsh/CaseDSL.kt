package io.rippledown.examples.vltsh

import io.rippledown.model.*

fun tshCase(lambda: CaseTemplate.() -> Unit): CaseTemplate {
    val template = CaseTemplate()
    template.lambda()
    return template
}

const val defaultTestDate = 1659752689505L

data class CaseTemplate(var name: String = "", var tsh: String = "", var freeT4: String = "") {
    private val defaultTSHRange = ReferenceRange("0.50", "4.0")
    private val defaultFreeT4Range = ReferenceRange("10", "20")
    private val defaultFreeT3Range = ReferenceRange("3.0", "5.5")
    var freeT3: String = ""
    var sex: String = "F"
    var age: Int = 0
    var location: String = "General Practice."
    var clinicalNotes: String = ""
    private var tests: String = "TFTs"
    private val extraResults = mutableMapOf<String, TestResult>()

    fun build(): RDRCase {
        val result = RDRCase(name)
        result.addValue("Sex", sex, defaultTestDate)
        result.addValue("Age", age.toString(), defaultTestDate)
        result.addResult("TSH", TestResult(Value(tsh), defaultTestDate, defaultTSHRange, " mU/L"))
        if (freeT4.isNotBlank()) {
            result.addResult("Free T4", TestResult(Value(freeT4), defaultTestDate, defaultFreeT4Range, " pmol/L"))
        }
        if (freeT3.isNotBlank()) {
            result.addResult("Free T3", TestResult(Value(freeT3), defaultTestDate, defaultFreeT3Range, " pmol/L"))
        }
        result.addValue("Patient Location", location, defaultTestDate)
        result.addValue("Tests", tests, defaultTestDate)
        result.addValue("Clinical Notes", clinicalNotes, defaultTestDate)
        extraResults.forEach {
            result.addResult(it.key, it.value)
        }
        return result
    }

    fun testValue(lambda: TestResultTemplate.() -> Unit): TestResultTemplate {
        val template = TestResultTemplate()
        template.lambda()
        extraResults[template.attribute] = template.result()
        return template
    }
}

class TestResultTemplate(var attribute: String = "", var value: String = "") {
    var lowerBound: String? = null
    private var upperBound: String? = null
    var units: String? = null

    fun result(): TestResult {
        var range: ReferenceRange? = null
        if (lowerBound != null || upperBound != null) {
            range = ReferenceRange(lowerBound, upperBound)
        }
        return TestResult(Value(value), defaultTestDate, range, units)
    }
}