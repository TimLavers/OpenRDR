package io.rippledown.examples.vltsh

import io.rippledown.model.RDRCase
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value

fun tshCase(lambda: CaseTemplate.() -> Unit): CaseTemplate {
    val template = CaseTemplate()
    template.lambda()
    return template
}

data class CaseTemplate(var name: String = "", var tsh: String = "", var freeT4: String = "") {
    private val defaultTSHRange = ReferenceRange("0.50", "4.0")
    private val defaultFreeT4Range = ReferenceRange("10", "20")
    var sex: String = "F"
    var age: Int = 0
    var location: String = "General Practice."
    var clinicalNotes: String = ""
    var tests: String = "TFTs"

    fun build(): RDRCase {
        val result = RDRCase(name)
        result.addValue("Sex", sex)
        result.addValue("Age", age.toString())
        result.addResult("TSH", TestResult(Value(tsh),defaultTSHRange, " mU/L" ))
        if (freeT4.isNotBlank()) {
            result.addResult("Free T4", TestResult(Value(freeT4),defaultFreeT4Range, " pmol/L" ))
        }
        result.addValue("Patient Location", location)
        result.addValue("Tests", tests)
        result.addValue("Clinical Notes", clinicalNotes)
        return result
    }
}