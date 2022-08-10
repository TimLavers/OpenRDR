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
        val result = RDRCaseBuilder()
        result.addValue("Sex", defaultTestDate, sex )
        result.addValue("Age", defaultTestDate, age.toString())
        result.addResult("TSH", defaultTestDate, TestResult(Value(tsh), defaultTSHRange, " mU/L"))
        if (freeT4.isNotBlank()) {
            result.addResult("Free T4", defaultTestDate, TestResult(Value(freeT4), defaultFreeT4Range, " pmol/L"))
        }
        if (freeT3.isNotBlank()) {
            result.addResult("Free T3", defaultTestDate, TestResult(Value(freeT3), defaultFreeT3Range, " pmol/L"))
        }
        result.addValue("Patient Location", defaultTestDate,  location)
        result.addValue("Tests", defaultTestDate, tests)
        result.addValue("Clinical Notes", defaultTestDate, clinicalNotes)
        extraResults.forEach {
            result.addResult(it.key, defaultTestDate, it.value)
        }
        return result.build(name)
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
        return TestResult(Value(value), range, units)
    }
}