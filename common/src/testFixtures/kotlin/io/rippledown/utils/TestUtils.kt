package io.rippledown.utils

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.rippledown.model.*
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.RuleSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
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

fun createViewableCase(caseId: CaseId, attributesWithValues: List<AttributeWithValue> = listOf(AttributeWithValue())) =
    createViewableCase(
        caseId.name,
        caseId.id,
        attributesWithValues
    )

fun createCase(
    name: String = "",
    caseId: Long? = null,
    attributesWithResults: List<AttributeWithValue> = listOf(AttributeWithValue())
) = with(RDRCaseBuilder()) {
    attributesWithResults.forEach {
        addResult(it.attribute, 99994322, it.result)
    }
    build(name, caseId)
}

fun createViewableCase(
    name: String = "",
    caseId: Long? = null,
    attributesWithResults: List<AttributeWithValue> = listOf(AttributeWithValue())
): ViewableCase {
    val case = createCase(name, caseId, attributesWithResults)
    val attributes = attributesWithResults.map { it.attribute }
    return ViewableCase(case, CaseViewProperties(attributes))
}

fun createViewableCaseWithInterpretation(
    name: String = "",
    caseId: Long? = null,
    conclusionTexts: List<String> = listOf(),
): ViewableCase {
    val case = createViewableCase(name, caseId, listOf(AttributeWithValue()))
    var conclusionId = 10
    val interp = Interpretation(case.case.caseId).apply {
        conclusionTexts.forEach { text ->
            add(
                RuleSummary(
                    conclusion = Conclusion(conclusionId++, text),
                    conditionTextsFromRoot = listOf(
                        "Condition 1 for $text",
                        "Condition 2 for $text",
                        "Condition 3 for $text"
                    )
                )
            )
        }
    }
    val text = interp.conclusionTexts().joinToString(" ")
    val viewableInterp =
        ViewableInterpretation(interpretation = interp, textGivenByRules = text)
    case.viewableInterpretation = viewableInterp
    return case
}

fun createCaseWithInterpretation(
    name: String = "",
    caseId: Long? = null,
    conclusionTexts: List<String> = listOf(),
): RDRCase {
    val commentToConditions = conclusionTexts.associateWith { emptyList<String>() }
    val interp = createInterpretation(commentToConditions)
    return createCase(name, caseId, listOf(AttributeWithValue())).apply { interpretation = interp }
}

fun createInterpretation(
    commentToConditions: Map<String, List<String>> = mapOf(),
): Interpretation {
    var conclusionId = 0
    return Interpretation().apply {
        commentToConditions.forEach { comment, conditions ->
            add(
                RuleSummary(
                    conclusion = Conclusion(conclusionId++, comment),
                    conditionTextsFromRoot = conditions
                )
            )
        }
    }
}

fun createViewableInterpretation(
    commentToConditions: Map<String, List<String>> = mapOf(),
): ViewableInterpretation {
    val interp = createInterpretation(commentToConditions)
    val text = interp.conclusionTexts().joinToString(" ")
    return ViewableInterpretation(interpretation = interp, textGivenByRules = text)
}

fun beSameAs(other: Condition) = Matcher<Condition> { value ->
    MatcherResult(
        value.sameAs(other),
        { "expected $other but got $value" },
        { "expected conditions not to be the same" },
    )
}

inline fun <reified T> serializeDeserialize(t: T): T {
    val json = Json {
        allowStructuredMapKeys = true
    }
    val serialized = json.encodeToString(t)
    return json.decodeFromString(serialized)
}

inline fun <reified T> checkSerializationIsThreadSafe(t: T) {
    runBlocking {
        repeat(10000) {
            launch(Dispatchers.Default) {
                serializeDeserialize(t)
            }
        }
    }
}