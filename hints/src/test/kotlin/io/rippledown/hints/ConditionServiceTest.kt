package io.rippledown.hints

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.rippledown.hints.ConditionService.conditionSpecificationsFor
import org.junit.jupiter.api.Test

class ConditionServiceTest {
    @Test
    fun `should generate 'high'`() {
        // Given
        val expressions = listOf(
            "x is elevated",
            "x is above the normal range",
            "raised x",
            "elevated x",
            "high x",
            "x es alto",
            "x es mejor que el rango normal"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(predicateName = "High", signatureName = "Current")
    }

    @Test
    fun `should generate 'low'`() {
        // Given
        val expressions = listOf(
            "x is lowered",
            "low x",
            "x is below the normal range",
            "x es menor que el rango normal",
            "x低於正常範圍"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(predicateName = "Low", signatureName = "Current")
    }

    @Test
    fun `should generate 'normal'`() {
        // Given
        val expressions = listOf(
            "x is OK",
            "x is not high or low",
            "x is within the normal range"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(predicateName = "Normal", signatureName = "Current")
    }

    @Test
    fun `should generate 'Is' with number`() {
        // Given
        val expressions = listOf(
            "x equals 3.1",
            "x = 3.1",
            "x == 3.1",
            "x is equal to 3.1"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "Is",
            predicateParameters = listOf("3.1"),
            signatureName = "Current"
        )
    }

    @Test
    fun `should generate 'Is' with unquoted text parameter`() {
        // Given
        val param = "abc"
        val expressions = listOf(
            "x is $param",
            "x = $param",
            "x == $param",
            "x is the same as $param",
            "x is equal to $param",
            "x identical to $param"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "Is",
            predicateParameters = listOf("\"$param\""),
            signatureName = "Current"
        )
    }

    @Test
    fun `should generate 'Is' with quoted text parameter`() {
        // Given
        val param = "\"abc\""
        val expressions = listOf(
            "x equals $param",
            "x = $param",
            "x == $param",
            "x is the same as $param",
            "x is equal to $param",
            "x identical to $param"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "Is",
            predicateParameters = listOf(param),
            signatureName = "Current"
        )
    }

    @Test
    fun `should generate 'greater than or equals'`() {
        // Given
        val expressions = listOf(
            "x is greater than or equal to 10.0",
            "x no less than 10.0",
            "x is greater than or equal to 10.0"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "GreaterThanOrEquals",
            predicateParameters = listOf("10.0"),
            signatureName = "Current"
        )
    }

    @Test
    fun `should generate 'less than or equals'`() {
        // Given
        val expressions = listOf(
            "x is less than or equal to 10.0",
            "x no more than 10.0",
            "x is smaller than or equal to 10.0"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "LessThanOrEquals",
            predicateParameters = listOf("10.0"),
            signatureName = "Current"
        )
    }

    @Test
    fun `should generate 'at most greater than or equals'`() {
        // Given
        val expressions = listOf(
            "at most 42 x are greater than or equal to 10.1",
            "no more than 42 x results greater than or equal to 10.1"
        )

        // When
        val actual = conditionSpecificationsFor(*expressions.toTypedArray())

        // Then
        actual shouldAllBe ConditionSpecification(
            predicateName = "GreaterThanOrEquals",
            predicateParameters = listOf("10.1"),
            signatureName = "AtMost",
            signatureParameters = listOf("42")
        )
    }

    private infix fun List<ConditionSpecification>.shouldAllBe(expected: ConditionSpecification) {
        forEach { actual ->
            withClue("Expected '$expected' but got '$actual'") {
                val res = actual.equals(expected)
                res shouldBe true
            }
        }
    }
}