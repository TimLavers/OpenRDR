package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RemoveReasonTest {
    private lateinit var ruleService: RuleService
    private lateinit var currentCase: ViewableCase
    private lateinit var modelResponder: ModelResponder

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
    }

    @Test
    fun `should remove the condition from the rule session`() = runTest {
        //Given
        val conditionText = "Sun is \"hot\""
        val action = RemoveReason(conditionText)

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.removeConditionByText(conditionText) }
    }

    @Test
    fun `should send CornerstoneStatus after removing a reason`() = runTest {
        //Given
        val conditionText = "Sun is \"hot\""
        val action = RemoveReason(conditionText)

        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.removeConditionByText(conditionText)
        } returns ccStatus

        val responseFromModel = ChatResponse("Reason removed.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}