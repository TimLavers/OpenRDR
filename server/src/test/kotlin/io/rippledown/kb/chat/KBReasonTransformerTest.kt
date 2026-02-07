package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.chat.ReasonTransformation
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KBReasonTransformerTest {

    private val case = mockk<RDRCase>()
    private val ruleService = mockk<RuleService>()
    private val modelResponder = mockk<ModelResponder>()
    private val transformer = KBReasonTransformer(case, ruleService, modelResponder)

    @Test
    fun `should add condition to rule session when a valid condition is parsed`() = runTest {
        // Given
        val reason = "glucose is elevated"
        val condition = greaterThanOrEqualTo(1, Attribute(1, "glucose"), 5.0)
        val parsingResult = ConditionParsingResult(condition)
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        verify { ruleService.addConditionToCurrentRuleSession(condition) }
    }

    @Test
    fun `should send cornerstone status when a valid condition is parsed`() = runTest {
        // Given
        val reason = "glucose is elevated"
        val condition = greaterThanOrEqualTo(1, Attribute(1, "glucose"), 5.0)
        val parsingResult = ConditionParsingResult(condition)
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        verify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should not add condition to rule session when no condition is parsed`() = runTest {
        // Given
        val reason = "something that cannot be parsed"
        val parsingResult = ConditionParsingResult(null, "error")
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
    }

    @Test
    fun `should not send cornerstone status when no condition is parsed`() = runTest {
        // Given
        val reason = "something that cannot be parsed"
        val parsingResult = ConditionParsingResult(null, "error")
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        verify(exactly = 0) { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return successful transformation when a valid condition is parsed`() = runTest {
        // Given
        val reason = "glucose is elevated"
        val condition = greaterThanOrEqualTo(1, Attribute(1, "glucose"), 5.0)
        val parsingResult = ConditionParsingResult(condition)
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        val result = transformer.transform(reason)

        // Then
        result.reasonId shouldBe condition.id()
        result.message shouldBe ReasonTransformation.TRANSFORMATION_MESSAGE.format(condition.asText())
    }

    @Test
    fun `should return failure transformation when no condition is parsed`() = runTest {
        // Given
        val reason = "something that cannot be parsed"
        val failureMessage = "Could not understand the reason"
        val parsingResult = ConditionParsingResult(null, failureMessage)
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        val result = transformer.transform(reason)

        // Then
        result.reasonId shouldBe null
        result.message shouldBe failureMessage
    }

    @Test
    fun `should return failure transformation with error message when parsing fails`() = runTest {
        // Given
        val reason = "something invalid"
        val failureMessage = "This expression is not valid for the current case"
        val parsingResult = ConditionParsingResult(null, failureMessage)
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        val result = transformer.transform(reason)

        // Then
        result.reasonId shouldBe null
        result.message shouldBe failureMessage
    }

    @Test
    fun `should call conditionForExpression with the correct case and reason`() = runTest {
        // Given
        val reason = "glucose is high"
        val parsingResult = ConditionParsingResult(null, "error")
        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        verify { ruleService.conditionForExpression(case, reason) }
    }

    @Test
    fun `should call modelResponder response with cornerstoneStatus when a condition is added`() = runTest {
        // Given
        val reason = "glucose is elevated"
        val condition = greaterThanOrEqualTo(1, Attribute(1, "glucose"), 5.0)
        val parsingResult = ConditionParsingResult(condition)
        val cornerstoneStatus = CornerstoneStatus()

        every { ruleService.conditionForExpression(case, reason) } returns parsingResult
        every { ruleService.cornerstoneStatus() } returns cornerstoneStatus

        // When
        transformer.transform(reason)

        // Then
        coVerify { modelResponder.response(cornerstoneStatus.toJsonString()) }
    }

    @Test
    fun `should not call modelResponder response when no condition is parsed`() = runTest {
        // Given
        val reason = "something that cannot be parsed"
        val parsingResult = ConditionParsingResult(null, "error")

        every { ruleService.conditionForExpression(case, reason) } returns parsingResult

        // When
        transformer.transform(reason)

        // Then
        coVerify(exactly = 0) { modelResponder.response(any()) }
    }
}
