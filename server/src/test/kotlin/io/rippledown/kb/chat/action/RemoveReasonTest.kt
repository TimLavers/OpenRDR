package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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

    @Test
    fun `should not tell the model to commit the rule when there are no cornerstones left`() = runTest {
        //Given a removal leaving no cornerstones to review
        val conditionText = "Sun is \"hot\""
        val action = RemoveReason(conditionText)
        coEvery { ruleService.removeConditionByText(conditionText) } returns CornerstoneStatus()
        coEvery { ruleService.sendCornerstoneStatus() } returns Unit
        val sentToModel = slot<String>()
        coEvery { modelResponder.response(capture(sentToModel)) } returns ChatResponse("Reason removed.")

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then the model is told to confirm the removal, not to commit. Removing a
        //reason is not the end of cornerstone review, and the commit directive used
        //by the review actions made the model commit the rule instead of replying.
        sentToModel.captured shouldContain "removed"
        sentToModel.captured shouldNotContain "CommitRule"
    }
}