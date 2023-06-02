package io.rippledown.model

import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.RuleSummary

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.rippledown.model.condition.Condition

import io.rippledown.model.condition.Condition

fun daysAgo(n: Int): Long {
    return defaultDate - n * 24 * 60 * 60 * 1000
}

fun randomString(length: Int): String {
    //https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z')
    return List(length) { alphabet.random() }.joinToString("")
}

const val defaultDate = 1659752689505
const val today = defaultDate
val yesterday = daysAgo(1)
val lastWeek = daysAgo(7)


fun createCase(name: String = ""): ViewableCase {
    val attribute = Attribute("Glucose", 1)
    val builder = RDRCaseBuilder()
    builder.addResult(attribute, 99994322, TestResult("5.1"))
    val rdrCase = builder.build(name)
    return ViewableCase(rdrCase, CaseViewProperties(listOf(attribute)))
}

fun createCaseWithInterpretation(
    name: String = "",
    conclusionTexts: List<String> = listOf(),
    diffs: DiffList = DiffList()
): ViewableCase {
    val case = createCase(name)
    var conclusionId = 10
    val interp = Interpretation(diffList = diffs).apply {
        conclusionTexts.forEach { text ->
            add(RuleSummary(conclusion = Conclusion(conclusionId++, text)))
        }
    }
    case.interpretation = interp
    return case
}

fun beSameAs(other: Condition) = Matcher<Condition> { value ->
    MatcherResult(
        value.sameAs(other),
        { "expected $other but got $value" },
        { "expected conditions not to be the same" },
    )
}

