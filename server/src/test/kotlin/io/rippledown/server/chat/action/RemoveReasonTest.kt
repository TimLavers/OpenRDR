package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.server.chat.ModelResponder
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RemoveReasonTest {
    private lateinit var ruleService: KbEditInterface
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
        val reasonId = 42
        val action = RemoveReason(reasonId)

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.removeCondition(reasonId) }
    }

    @Test
    fun `should send CornerstoneStatus after removing a reason`() = runTest {
        //Given
        val reasonId = 42
        val action = RemoveReason(reasonId)

        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.removeCondition(reasonId)
        } returns ccStatus

        val responseFromModel = "Reason removed."
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}