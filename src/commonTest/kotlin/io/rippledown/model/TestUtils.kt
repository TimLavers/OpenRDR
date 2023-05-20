package io.rippledown.model

import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.diff.DiffList
import io.rippledown.model.rule.RuleSummary

fun daysAgo(n: Int): Long {
    return defaultDate - n * 24 * 60 * 60 * 1000
}

const val defaultDate = 1659752689505
const val today = defaultDate
val yesterday = daysAgo(1)
val lastWeek = daysAgo(7)


fun createCase(name: String = ""): ViewableCase {
    val attribute = Attribute("Glucose")
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
    val interp = Interpretation(diffList = diffs).apply {
        conclusionTexts.forEach { text ->
            add(RuleSummary(conclusion = Conclusion(text)))
        }
    }
    case.interpretation = interp
    return case
}