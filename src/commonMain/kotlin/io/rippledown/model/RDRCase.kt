package io.rippledown.model

import io.ktor.util.*
import kotlinx.serialization.Serializable

/*
 Set of (Attribute, TestResult) pairs in which
 no two pairs contain have the same attribute
 and test results with the same date.
 The case can be organised as a table of TestResults,
 with each column representing TestResults with
 the same date, columns ordered chronologically,
 and rows representing results for the same attribute.
 In this arrangement, some entries will be blank if
 not all attributes got results on all of the dates.

 So is {Attribute, Date} a fundamental? Do we want
 {Attribute, Date} -> TestResult as the data structure.
 What is {Attribute, Date}??

 */

class RDRCaseBuilder {
    private val caseData: MutableMap<TestEvent, TestResult> = mutableMapOf()

    fun addValue(attribute: String, date: Long, value: String) {
        addResult(attribute, date, TestResult(value, date))
    }

    fun addResult(attribute: String, date: Long, result: TestResult) {
        val testEvent = TestEvent(Attribute(attribute), date)
        caseData[testEvent] = result
    }

    fun build(name: String): RDRCase {
        return RDRCase(name, caseData)
    }
}

@Serializable
data class RDRCase(val name: String, val data: Map<TestEvent, TestResult>) {

    val caseData: Map<String, TestResult> = data.entries.associate { it.key.attribute.name to it.value }.toMap()
    val dates: List<Long>
    private val attributes: Set<Attribute>
    private val dateToEpisode: Map<Long, Map<Attribute, TestResult>>

    init {
        val uniqueDates = mutableSetOf<Long>()
        data.keys.forEach { uniqueDates.add(it.date) }
        dates = uniqueDates.sorted()
        attributes = data.keys.map { it.attribute }.toSet()
        val dateToEpisodeMutable = mutableMapOf<Long, Map<Attribute, TestResult>>()

        dates.forEach {
            val attributeMap = mutableMapOf<Attribute, TestResult>()

            attributes.forEach { attribute ->
                val key = TestEvent(attribute, it)
                val result = data[key] ?: TestResult("", it)
                attributeMap[attribute] = result
            }
            dateToEpisodeMutable[it] = attributeMap.toMap()
        }
        dateToEpisode = dateToEpisodeMutable.toMap()
    }

    fun values(attributeName: String): List<TestResult>? {
        val attribute = Attribute(attributeName)
        if (!attributes.contains(attribute)) {
            return null
        }
        val result = mutableListOf<TestResult>()
        dates.forEach {
            result.add(dateToEpisode[it]!![attribute]!!)
        }
        return result
    }

    fun get(attribute: String): TestResult? {
        return caseData[attribute]
    }

    fun getLatest(attribute: Attribute): TestResult? {
        return caseData[attribute.name]
    }
}