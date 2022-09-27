package io.rippledown.examples.vltsh

import io.rippledown.model.*
import kotlinx.datetime.Instant

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

fun multiEpisodeCase(lambda: MultiEpisodeCaseTemplate.() -> Unit): MultiEpisodeCaseTemplate {
    val template = MultiEpisodeCaseTemplate()
    template.lambda()
    return template
}

data class MultiEpisodeCaseTemplate(var name: String = "") {
    var dates: List<Long> = emptyList()
    var attributeToValues = mutableMapOf<Attribute, List<TestResult>>()
    var sex = "F"

    fun build(): RDRCase {
        val builder = RDRCaseBuilder()
        val numberOfEpisodes = dates.size
        require(dates.isNotEmpty()) { "dates should not be empty"}
        attributeToValues.forEach {
            require(it.value.size == numberOfEpisodes) { "Each attribute should have $numberOfEpisodes values, ${it.key} has ${it.value.size}." }
        }
        dates.forEachIndexed { index, date ->
            attributeToValues.forEach {
                val testResult = it.value[index]
                builder.addResult(it.key, date, testResult)
            }
            builder.addResult("Sex", date,TestResult( sex))
        }
        return builder.build(name)
    }

    fun dates(lambda: DatesTemplate.() -> Unit): DatesTemplate {
        val template = DatesTemplate()
        template.lambda()
        dates = template.dateList()
        return template
    }

    fun testValues(lambda: TestResultsTemplate.() -> Unit): TestResultsTemplate {
        val template = TestResultsTemplate()
        template.lambda()
        attributeToValues[Attribute(template.attribute)] = template.results()
        return template
    }

    fun clinicalNotes(lambda: ClinicalNotesTemplate.() -> Unit): ClinicalNotesTemplate {
        val template = ClinicalNotesTemplate()
        template.lambda()
        attributeToValues[Attribute(template.attribute)] = template.results()
        return template
    }
}

class DatesTemplate(var datesCSL: String = "") {

    fun dateList(): List<Long> {
        println("datesCSL: $datesCSL")
        val dateList = mutableListOf<Long>()
        datesCSL.split(",").forEach { dateList.add(parseDate(it.trim())) }
        return dateList
    }

    private fun parseDate(dateString: String): Long {
        return Instant.parse(dateString).toEpochMilliseconds()
    }
}
class TestResultsTemplate(var attribute: String = "", var valuesCSL: String = "") {
    var lowerBound: String? = null
    var upperBound: String? = null
    var units: String? = null

    fun results(): List<TestResult> {
        var range: ReferenceRange? = null
        if (lowerBound != null || upperBound != null) {
            range = ReferenceRange(lowerBound, upperBound)
        }
        return valuesCSL.split(",").map{ s: String -> TestResult(s.trim(), range, units) }
    }
}
class ClinicalNotesTemplate(var attribute: String = "Clinical Notes", var values_separated: String = "") {

    fun results(): List<TestResult> {
        return values_separated.split("_").map { s: String -> TestResult(s.trim(), null, null) }
    }
}
