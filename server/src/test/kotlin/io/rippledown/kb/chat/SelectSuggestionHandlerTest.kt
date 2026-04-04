package io.rippledown.kb.chat

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.chat.Conversation.Companion.CONDITION_TEXT_PARAMETER
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SelectSuggestionHandlerTest {
    private lateinit var ruleService: RuleService
    private lateinit var case: RDRCase

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        case = mockk()
    }

    @Test
    fun `should add matching non-editable condition to rule session`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify { ruleService.addConditionToCurrentRuleSession(condition) }
    }

    @Test
    fun `should send cornerstone status after adding condition`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return result containing reasonId`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val handler = SelectSuggestionHandler(case, ruleService)
        val condition = mockk<Condition>()
        every { condition.id() } returns 42
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        every { ruleService.cornerstoneStatus() } returns CornerstoneStatus()
        every { ruleService.sendCornerstoneStatus() } returns Unit

        // When
        val result = handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        result shouldContain "reasonId"
        result shouldContain "42"
        result shouldContain "Ok"
        result shouldContain "Cornerstone status:"
    }

    @Test
    fun `should return fallback message when no matching suggestion found`() = runTest {
        // Given
        val conditionText = "unknown condition"
        val handler = SelectSuggestionHandler(case, ruleService)
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns null

        // When
        val result = handler.handle(mapOf(CONDITION_TEXT_PARAMETER to conditionText))

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        result shouldContain "Could not find a matching non-editable suggestion"
        result shouldContain conditionText
        result shouldNotContain "reasonId\":42"
    }

    @Test
    fun `should handle empty condition text`() = runTest {
        // Given
        val handler = SelectSuggestionHandler(case, ruleService)
        every { ruleService.conditionForSuggestionText(case, "") } returns null

        // When
        val result = handler.handle(emptyMap())

        // Then
        verify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        result shouldContain "Could not find a matching non-editable suggestion"
    }
}
