package io.rippledown.hints

import io.kotest.matchers.shouldBe
import io.rippledown.hints.ConditionService.conditionSpecificationsFor
import org.junit.jupiter.api.Test

class ConditionServiceTest {
    @Test
    fun `should generate conditions from expressions`() {
        // Given
        val high = ConditionSpecification(predicateName = "High", signatureName = "Current")
        val low = ConditionSpecification(predicateName = "Low", signatureName = "Current")
        val normal = ConditionSpecification(predicateName = "Normal", signatureName = "Current")
        val isFloat = ConditionSpecification(
            predicateName = "Is",
            predicateParameters = listOf("3.1"),
            signatureName = "Current"
        )
        val text = "abc"
        val quotedText = "\"abc\""

        val isText = ConditionSpecification(
            predicateName = "Is",
            predicateParameters = listOf("\"$text\""),
            signatureName = "Current"
        )

        val gte10 = ConditionSpecification(
            predicateName = "GreaterThanOrEquals",
            predicateParameters = listOf("10.0"),
            signatureName = "Current"
        )
        val lte10 = ConditionSpecification(
            predicateName = "LessThanOrEquals",
            predicateParameters = listOf("10.0"),
            signatureName = "Current"
        )
        val atMostGTE = ConditionSpecification(
            predicateName = "GreaterThanOrEquals",
            predicateParameters = listOf("10.1"),
            signatureName = "AtMost",
            signatureParameters = listOf("42")
        )

        val expressionToCondition = sortedMapOf(
            "x is elevated" to high,
            "x is above the normal range" to high,
            "raised x" to high,
            "elevated x" to high,
            "high x" to high,
            "x es alto" to high,
            "x es mejor que el rango normal" to high,
            "x is lowered" to low,
            "low x" to low,
            "x is below the normal range" to low,
            "x es menor que el rango normal" to low,
            "x低於正常範圍" to low,
            "x is OK" to normal,
            "x is not high or low" to normal,
            "x is within the normal range" to normal,
            "x equals 3.1" to isFloat,
            "x = 3.1" to isFloat,
            "x == 3.1" to isFloat,
            "x is equal to 3.1" to isFloat,
            "x is $text" to isText,
            "x = $text" to isText,
            "x == $text" to isText,
            "x is the same as $text" to isText,
            "x is equal to $text" to isText,
            "x identical to $text" to isText,
            "x equals $quotedText" to isText,
            "x = $quotedText" to isText,
            "x == $quotedText" to isText,
            "x is the same as $quotedText" to isText,
            "x is equal to $quotedText" to isText,
            "x identical to $quotedText" to isText,
            "x is greater than or equal to 10.0" to gte10,
            "x no less than 10.0" to gte10,
            "x is greater than or equal to 10.0" to gte10,
            "x is less than or equal to 10.0" to lte10,
            "x no more than 10.0" to lte10,
            "x is smaller than or equal to 10.0" to lte10,
            "at most 42 x are greater than or equal to 10.1" to atMostGTE,
            "no more than 42 x results greater than or equal to 10.1" to atMostGTE
        )

        // When
        val actual = conditionSpecificationsFor(*expressionToCondition.keys.toTypedArray())

        // Then
        val expressions = expressionToCondition.keys.toList()
        actual.forEachIndexed { index, condition ->
            val key = expressions[index]
            val expectedCondition = expressionToCondition[key]
            condition shouldBe expectedCondition
        }
    }
}