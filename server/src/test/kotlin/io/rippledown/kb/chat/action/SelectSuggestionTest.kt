package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SelectSuggestionTest {
    private lateinit var ruleService: RuleService
    private lateinit var currentCase: ViewableCase
    private lateinit var modelResponder: ModelResponder
    private lateinit var case: RDRCase

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
        case = mockk()
        every { currentCase.case } returns case
    }

    @Test
    fun `should add matching non-editable condition to rule session`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val action = SelectSuggestion(conditionText)
        val condition = mockk<Condition>()
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 2)
        every { ruleService.cornerstoneStatus() } returns ccStatus
        every { ruleService.sendCornerstoneStatus() } returns Unit
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        val responseFromModel = ChatResponse("Condition added.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        // When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        // Then
        coVerify { ruleService.addConditionToCurrentRuleSession(condition) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send cornerstone status after adding condition`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val action = SelectSuggestion(conditionText)
        val condition = mockk<Condition>()
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 2)
        every { ruleService.cornerstoneStatus() } returns ccStatus
        every { ruleService.sendCornerstoneStatus() } returns Unit
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        coEvery { modelResponder.response(any<String>()) } returns ChatResponse("ok")

        // When
        action.doIt(ruleService, currentCase, modelResponder)

        // Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should pass cornerstone summary to model responder`() = runTest {
        // Given
        val conditionText = "ABC is not in case"
        val action = SelectSuggestion(conditionText)
        val condition = mockk<Condition>()
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns condition
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 1, numberOfCornerstones = 3)
        every { ruleService.cornerstoneStatus() } returns ccStatus
        every { ruleService.sendCornerstoneStatus() } returns Unit
        every { ruleService.addConditionToCurrentRuleSession(condition) } returns Unit
        coEvery { modelResponder.response(any<String>()) } returns ChatResponse("ok")

        // When
        action.doIt(ruleService, currentCase, modelResponder)

        // Then
        coVerify { modelResponder.response(ccStatus.summary()) }
    }

    @Test
    fun `should fall back to model responder when no matching suggestion found`() = runTest {
        // Given
        val conditionText = "unknown condition"
        val action = SelectSuggestion(conditionText)
        every { ruleService.conditionForSuggestionText(case, conditionText) } returns null
        val fallbackResponse = ChatResponse("Use transformReasonToFormalCondition instead.")
        coEvery { modelResponder.response(any<String>()) } returns fallbackResponse

        // When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        // Then
        coVerify(exactly = 0) { ruleService.addConditionToCurrentRuleSession(any()) }
        coVerify {
            modelResponder.response(withArg { message ->
                message shouldContain "Could not find a matching non-editable suggestion"
                message shouldContain conditionText
            })
        }
        response shouldBe fallbackResponse
    }

    @Test
    fun `should throw when no current case`() = runTest {
        // Given
        val action = SelectSuggestion("some condition")

        // When/Then
        assertFailsWith<IllegalStateException> {
            action.doIt(ruleService, null, modelResponder)
        }
    }
}
