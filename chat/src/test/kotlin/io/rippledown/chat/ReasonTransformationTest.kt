package io.rippledown.chat

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.signature.Current
import org.junit.jupiter.api.Test

class ReasonTransformationTest {
    private val condition = EpisodicCondition(7, Attribute(1, "Glucose"), High, Current, "elevated glucose")

    @Test
    fun `mentions the stored phrase when the incoming phrase differs from both texts`() {
        // Given
        val parsed = ConditionParsingResult(condition, expression = "raised glucose")

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(
            7, "Added your reason 'Glucose is high' (you previously called this 'elevated glucose')."
        )
    }

    @Test
    fun `typing the formal text keeps the existing transformation message`() {
        // Given
        val parsed = ConditionParsingResult(condition, expression = condition.asText())

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(7, "Added your reason 'Glucose is high'.")
    }

    @Test
    fun `typing the stored phrase keeps the existing transformation message`() {
        // Given
        val parsed = ConditionParsingResult(condition, expression = "elevated glucose")

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(7, "Added your reason 'Glucose is high'.")
    }

    @Test
    fun `a missing incoming expression preserves the legacy response`() {
        // Given
        val parsed = ConditionParsingResult(condition)

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(7, "Added your reason 'Glucose is high'.")
    }

    @Test
    fun `a whitespace-only incoming expression does not trigger the note`() {
        // Given
        val parsed = ConditionParsingResult(condition, expression = " \t\n")

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(7, "Added your reason 'Glucose is high'.")
    }

    @Test
    fun `a blank stored phrase does not trigger the note`() {
        // Given
        val conditions = listOf("", " \t\n").map { condition.copy(userExpression = it) }

        // When
        val results =
            conditions.map { ConditionParsingResult(it, expression = "raised glucose").toExpressionTransformation() }

        // Then
        results.forEach { it shouldBe ReasonTransformation(7, "Added your reason 'Glucose is high'.") }
    }

    @Test
    fun `matching formal and stored texts still give Ok`() {
        // Given
        val parsed = ConditionParsingResult(
            condition.copy(userExpression = condition.asText()), expression = condition.asText()
        )

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(7, "Ok")
    }

    @Test
    fun `a different incoming phrase takes precedence even when the stored phrase is formal`() {
        // Given
        val parsed = ConditionParsingResult(
            condition.copy(userExpression = condition.asText()), expression = "raised glucose"
        )

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(
            7, "Added your reason 'Glucose is high' (you previously called this 'Glucose is high')."
        )
    }

    @Test
    fun `a validation failure takes precedence over the previous phrase note`() {
        // Given
        val parsed = ConditionParsingResult(condition, "Not true for this case", "raised glucose")

        // When
        val result = parsed.toExpressionTransformation()

        // Then
        result shouldBe ReasonTransformation(message = "Not true for this case")
    }
}
