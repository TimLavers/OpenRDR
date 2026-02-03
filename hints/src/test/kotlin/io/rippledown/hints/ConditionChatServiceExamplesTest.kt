package io.rippledown.hints

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConditionChatServiceExamplesTest {
    private lateinit var service: ConditionChatService
    val attributeNames = listOf("x", "glucose")

    @BeforeEach
    fun setUp() {
        service = ConditionChatService()
    }

    fun cs(
        expression: String,
        attribute: String?,
        predicate: String,
        predicateParameters: List<String> = listOf(),
        signature: String,
        signatureParameters: List<String> = listOf()
    ) =
        expression to ConditionSpecification(
            userExpression = expression,
            attributeName = attribute,
            predicateName = predicate,
            predicateParameters = predicateParameters,
            signatureName = signature,
            signatureParameters = signatureParameters
        )

    private suspend fun verify(expressionToCondition: Map<String, ConditionSpecification>) {
        expressionToCondition.forEach { (expression, expectedCondition) ->
            val actualCondition = service.transform(expression, attributeNames)
            actualCondition shouldBe expectedCondition
        }
    }

    @Test
    fun `should generate High conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x is elevated", "x", "High", signature = "Current"),
                cs("elevated glucose", "glucose", "High", signature = "Current"),
                cs("x is above the normal range", "x", "High", signature = "Current"),
                cs("raised x", "x", "High", signature = "Current"),
                cs("elevated x", "x", "High", signature = "Current"),
                cs("high x", "x", "High", signature = "Current"),
                cs("x es mejor que el rango normal", "x", "High", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate Low conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x is lowered", "x", "Low", signature = "Current"),
                cs("low x", "x", "Low", signature = "Current"),
                cs("x is below the normal range", "x", "Low", signature = "Current"),
                cs("x es menor que el rango normal", "x", "Low", signature = "Current"),
                cs("x低於正常範圍", "x", "Low", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate Normal conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x is OK", "x", "Normal", signature = "Current"),
                cs("x is not high or low", "x", "Normal", signature = "Current"),
                cs("x is within the normal range", "x", "Normal", signature = "Current"),
                cs("x is within the acceptable range", "x", "Normal", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate HighOrNormal conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose is either high or normal", "glucose", "HighOrNormal", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate Is conditions with numeric values`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x equals 3.1", "x", "Is", listOf("3.1"), "Current"),
                cs("x = 3.1", "x", "Is", listOf("3.1"), "Current"),
                cs("x == 3.1", "x", "Is", listOf("3.1"), "Current"),
                cs("x is equal to 3.1", "x", "Is", listOf("3.1"), "Current"),
            )
        )
    }

    @Test
    fun `should generate Is conditions with text values`() = runBlocking {
        val text = "abc"
        verify(
            sortedMapOf(
                cs("x is $text", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x = $text", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x == $text", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x is the same as $text", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x is equal to $text", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x identical to $text", "x", "Is", listOf("\"$text\""), "Current"),
            )
        )
    }

    @Test
    fun `should generate Is conditions with quoted text values`() = runBlocking {
        val text = "abc"
        val quotedText = "\"abc\""
        verify(
            sortedMapOf(
                cs("x equals $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x = $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x == $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x is the same as $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x is equal to $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
                cs("x identical to $quotedText", "x", "Is", listOf("\"$text\""), "Current"),
            )
        )
    }

    @Test
    fun `should generate IsNot conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x does not equal 3.1", "x", "IsNot", listOf("3.1"), "Current"),
                cs("x isn't \"pending\"", "x", "IsNot", listOf("\"pending\""), "Current"),
            )
        )
    }
    @Test
    fun `should generate GreaterThanOrEquals conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x is greater than or equal to 10.0", "x", "GreaterThanOrEquals", listOf("10.0"), "Current"),
                cs("x no less than 10.0", "x", "GreaterThanOrEquals", listOf("10.0"), "Current"),
                cs("x is at least 3.14159", "x", "GreaterThanOrEquals", listOf("3.14159"), "Current"),
            )
        )
    }

    @Test
    fun `should generate LessThanOrEquals conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("x is less than or equal to 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
                cs("x no more than 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
                cs("x is smaller than or equal to 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
            )
        )
    }

    @Test
    fun `should generate LessThan conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose smaller than 3.14159", "glucose", "LessThan", listOf("3.14159"), "Current"),
            )
        )
    }

    @Test
    fun `should generate GreaterThan conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose more than 3.14159", "glucose", "GreaterThan", listOf("3.14159"), "Current"),
            )
        )
    }

    @Test
    fun `should recognise a misspelled attribute name`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glcuose more than 3.14159", "glucose", "GreaterThan", listOf("3.14159"), "Current"),
            )
        )
    }

    @Test
    fun `should generate AtMost conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "at most 42 x are greater than or equal to 10.1",
                    "x",
                    "GreaterThanOrEquals",
                    listOf("10.1"),
                    "AtMost",
                    listOf("42")
                ),
                cs(
                    "no more than 42 x results greater than or equal to 10.1",
                    "x",
                    "GreaterThanOrEquals",
                    listOf("10.1"),
                    "AtMost",
                    listOf("42")
                ),
                cs(
                    "no more than 5 x results are elevated",
                    "x",
                    "High",
                    listOf(),
                    "AtMost",
                    listOf("5")
                ),
            )
        )
    }

    @Test
    fun `should generate IsNumeric conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose is a number", "glucose", "IsNumeric", listOf(), "Current"),
            )
        )
    }

    @Test
    fun `should generate IsPresentInCase conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose is available", "glucose", "IsPresentInCase", listOf(), ""),
            )
        )
    }

    @Test
    fun `should generate IsAbsentFromCase conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose is not available", "glucose", "IsAbsentFromCase", listOf(), ""),
            )
        )
    }

    @Test
    fun `should generate IsSingleEpisodeCase conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("case has only one episode", null, "IsSingleEpisodeCase", listOf(), ""),
            )
        )
    }

    @Test
    fun `should generate AtLeast conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("at least 2 x are less than 10", "x", "LessThan", listOf("10"), "AtLeast", listOf("2")),
            )
        )
    }

    @Test
    fun `should generate All conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("every glucose result is normal", "glucose", "Normal", listOf(), "All", listOf()),
                cs("every glucose result is elevated", "glucose", "High", listOf(), "All", listOf()),
                cs("every glucose result is a number", "glucose", "IsNumeric", listOf(), "All", listOf()),
            )
        )
    }

    @Test
    fun `should generate No conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("every glucose result is abnormal", "glucose", "Normal", listOf(), "No", listOf()),
                cs("no elevated glucose", "glucose", "High", listOf(), "No", listOf()),
                cs("no glucose contains undefined", "glucose", "Contains", listOf("\"undefined\""), "No", listOf()),
                cs("no glucose contains \"undefined\"", "glucose", "Contains", listOf("\"undefined\""), "No", listOf()),
                cs("none of the glucose results are numeric", "glucose", "IsNumeric", listOf(), "No", listOf()),
            )
        )
    }

    @Test
    fun `should generate Contains conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs("glucose contains undefined", "glucose", "Contains", listOf("\"undefined\""), "Current"),
                cs("glucose contains \"undefined\"", "glucose", "Contains", listOf("\"undefined\""), "Current"),
            )
        )
    }

    @Test
    fun `should generate DoesNotContain conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "glucose does not contain undefined",
                    "glucose",
                    "DoesNotContain",
                    listOf("\"undefined\""),
                    "Current"
                ),
                cs(
                    "glucose does not contain \"undefined\"",
                    "glucose",
                    "DoesNotContain",
                    listOf("\"undefined\""),
                    "Current"
                ),
            )
        )
    }

    @Test
    fun `should generate LowByAtMostSomePercentage conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "glucose is lowered by no more than 15%",
                    "glucose",
                    "LowByAtMostSomePercentage",
                    listOf("15"),
                    "Current"
                )
            )
        )
    }

    @Test
    fun `should generate HighByAtMostSomePercentage conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "glucose is elevated by no more than 15%",
                    "glucose",
                    "HighByAtMostSomePercentage",
                    listOf("15"),
                    "Current"
                )
            )
        )
    }

    @Test
    fun `should generate NormalOrLowByAtMostSomePercentage conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "glucose is either normal or not more than 15 percent below normal",
                    "glucose",
                    "NormalOrLowByAtMostSomePercentage",
                    listOf("15"),
                    "Current"
                )
            )
        )
    }

    @Test
    fun `should generate NormalOrHighByAtMostSomePercentage conditions`() = runBlocking {
        verify(
            sortedMapOf(
                cs(
                    "glucose is either normal or not more than 15 percent above normal",
                    "glucose",
                    "NormalOrHighByAtMostSomePercentage",
                    listOf("15"),
                    "Current"
                )
            )
        )
    }
}
