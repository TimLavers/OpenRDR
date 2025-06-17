package io.rippledown.chat

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.toJsonString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ExpressionEvaluationTest {
    @Test
    fun `should create an ExpressionEvaluation from a valid ConditionParsingResult where the user expression is the same as the condition text`() {
        //Given
        val condition = mockk<Condition>().apply {
            val expression = "Temperature > 30"
            every { asText() } returns expression
            every { userExpression() } returns expression
        }
        val conditionParsingResult = ConditionParsingResult(condition = condition)

        // When
        val expressionEvaluation = conditionParsingResult.toExpressionEvaluation()

        // Then
        expressionEvaluation.isValid shouldBe true
        expressionEvaluation.message shouldBe ExpressionEvaluation.VALID_CONDITION_MESSAGE
    }

    @Test
    fun `should create an ExpressionEvaluation from a valid ConditionParsingResult where the user expression is different to the condition text`() {
        //Given
        val condition = mockk<Condition>().apply {
            val expression = "It's hot today"
            val condition = "Temperature > 30"
            every { userExpression() } returns expression
            every { asText() } returns condition
        }
        val conditionParsingResult = ConditionParsingResult(condition = condition)

        // When
        val expressionEvaluation = conditionParsingResult.toExpressionEvaluation()

        // Then
        expressionEvaluation.isValid shouldBe true
        expressionEvaluation.message shouldBe ExpressionEvaluation.VALID_CONDITION_WITH_EVALUATION_MESSAGE.format(
            condition.asText()
        )
    }

    @Test
    fun `should create an ExpressionEvaluation from an invalid ConditionParsingResult`() {
        // Given
        val errorMessage = "Invalid condition syntax"
        val conditionParsingResult = ConditionParsingResult(condition = null, errorMessage = errorMessage)

        // When
        val expressionEvaluation = conditionParsingResult.toExpressionEvaluation()

        // Then
        expressionEvaluation.isValid shouldBe false
        expressionEvaluation.message shouldBe errorMessage
    }

    @Test
    fun `should be able to serialise ExpressionEvaluation to JSON`() {
        // Given
        val expressionEvaluation = ExpressionEvaluation(true, "Valid condition")

        // When
        val result = expressionEvaluation.toJsonString()

        // Then
        Json.parseToJsonElement(result) shouldBe Json.parseToJsonElement("""{"isValid":true,"message":"Valid condition"}""")
    }

}