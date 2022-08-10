package io.rippledown.model

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

    fun get(attribute: Attribute): TestResult? {
        return caseData[attribute.name]
    }
    fun get(attribute: String): TestResult? {
        return caseData[attribute]
    }

    fun getLatest(attribute: Attribute): TestResult? {
        return caseData[attribute.name]
    }
}