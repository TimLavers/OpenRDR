package io.rippledown.model

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.diff.DiffList
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlin.system.measureTimeMillis

fun daysAgo(n: Int): Long {
    return defaultDate - n * 24 * 60 * 60 * 1000
}

fun daysAfter(n: Int): Long {
    return defaultDate + n * 24 * 60 * 60 * 1000
}

fun randomString(length: Int): String {
    //https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z')
    return List(length) { alphabet.random() }.joinToString("")
}

fun printDuration(text: String, block: () -> Unit) {
    System.out.print("${"Millis for $text"}: ${measureTimeMillis(block)}\n")
}

const val defaultDate = 1659752689505

const val today = defaultDate
val yesterday = daysAgo(1)
val lastWeek = daysAgo(7)
val glucose = Attribute(1, "Glucose")

data class AttributeWithValue(val attribute: Attribute = glucose, val result: TestResult = TestResult("5.1"))

fun createCase(caseId: CaseId, attributesWithValues: List<AttributeWithValue> = listOf(AttributeWithValue())) =
    createCase(
        caseId.name,
        caseId.id,
        attributesWithValues
    )

fun createCase(
    name: String = "",
    id: Long? = null,
    attributesWithResults: List<AttributeWithValue> = listOf(AttributeWithValue())
): ViewableCase {
    val builder = RDRCaseBuilder()
    attributesWithResults.forEach {
        builder.addResult(it.attribute, 99994322, it.result)
    }
    val rdrCase = builder.build(name, id)
    val attributes = attributesWithResults.map { it.attribute }
    return ViewableCase(rdrCase, CaseViewProperties(attributes))
}

fun createCaseWithInterpretation(
    name: String = "",
    id: Long? = null,
    conclusionTexts: List<String> = listOf(),
    diffs: DiffList = DiffList()
): ViewableCase {
    val case = createCase(name, id, listOf(AttributeWithValue()))
    var conclusionId = 10
    val interp = Interpretation(case.case.caseId).apply {
        conclusionTexts.forEach { text ->
            add(RuleSummary(conclusion = Conclusion(conclusionId++, text)))
        }
    }
    val text = interp.conclusionTexts().joinToString(" ")
    val viewableInterp =
        ViewableInterpretation(interpretation = interp, verifiedText = null, diffList = diffs, textGivenByRules = text)
    case.viewableInterpretation = viewableInterp
    return case
}

fun beSameAs(other: Condition) = Matcher<Condition> { value ->
    MatcherResult(
        value.sameAs(other),
        { "expected $other but got $value" },
        { "expected conditions not to be the same" },
    )
}

