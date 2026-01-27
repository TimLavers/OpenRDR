package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.rippledown.hints.ConditionService.conditionSpecificationsFor
import org.junit.jupiter.api.Test

class ConditionServiceTest {
    fun cs(
        expression: String,
        attribute: String,
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

    private fun verify(expressionToCondition: Map<String, ConditionSpecification>) {
        val inputExpressions = expressionToCondition.keys.toTypedArray()
        val actual = conditionSpecificationsFor(*inputExpressions)
        actual.forEachIndexed { index, condition ->
            val key = inputExpressions[index]
            val expectedCondition = expressionToCondition[key]
            condition shouldBe expectedCondition
        }
    }

    @Test
    fun `should generate High conditions`() {
        verify(
            sortedMapOf(
                cs("x is elevated", "x", "High", signature = "Current"),
                cs("x is above the normal range", "x", "High", signature = "Current"),
                cs("raised x", "x", "High", signature = "Current"),
                cs("elevated x", "x", "High", signature = "Current"),
                cs("high x", "x", "High", signature = "Current"),
                cs("x es mejor que el rango normal", "x", "High", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate Low conditions`() {
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
    fun `should generate Normal conditions`() {
        verify(
            sortedMapOf(
                cs("x is OK", "x", "Normal", signature = "Current"),
                cs("x is not high or low", "x", "Normal", signature = "Current"),
                cs("x is within the normal range", "x", "Normal", signature = "Current"),
            )
        )
    }

    @Test
    fun `should generate Is conditions with numeric values`() {
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
    fun `should generate Is conditions with text values`() {
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
    fun `should generate Is conditions with quoted text values`() {
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
    fun `should generate GreaterThanOrEquals conditions`() {
        verify(
            sortedMapOf(
                cs("x is greater than or equal to 10.0", "x", "GreaterThanOrEquals", listOf("10.0"), "Current"),
                cs("x no less than 10.0", "x", "GreaterThanOrEquals", listOf("10.0"), "Current"),
            )
        )
    }

    @Test
    fun `should generate LessThanOrEquals conditions`() {
        verify(
            sortedMapOf(
                cs("x is less than or equal to 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
                cs("x no more than 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
                cs("x is smaller than or equal to 10.0", "x", "LessThanOrEquals", listOf("10.0"), "Current"),
            )
        )
    }

    @Test
    fun `should generate AtMost conditions`() {
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
            )
        )
    }

    @Test
    fun `should generate AtLeast conditions`() {
        verify(
            sortedMapOf(
                cs("at least 2 x are less than 10", "x", "LessThan", listOf("10"), "AtLeast", listOf("2")),
            )
        )
    }
}
