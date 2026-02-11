package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AddCommentTest {
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
    fun `should start a rule session to add a comment`() = runTest {
        //Given
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd)
        } returns ccStatus

        val responseFromModel = "There are 84 cornerstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.toJsonString<CornerstoneStatus>()) }
        response shouldBe responseFromModel
    }
    @Test
    fun `should send CornerstoneStatus after starting a rule session to add a comment`() = runTest {
        //Given
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd)
        } returns ccStatus

        val responseFromModel = "There are 84 cornstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }
}