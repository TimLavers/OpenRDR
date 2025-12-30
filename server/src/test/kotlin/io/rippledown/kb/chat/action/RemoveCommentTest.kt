package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RemoveCommentTest {
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
    fun `should start a rule session to remove a comment`() = runTest {
        //Given
        val commentToRemove = "please remove me"
        val action = RemoveComment(commentToRemove)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToRemoveComment(any(), commentToRemove)
        } returns ccStatus

        val responseFromModel = "There are 84 cornstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.toJsonString<CornerstoneStatus>()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send CornerstoneStatus after starting a rule session to remove a comment`() = runTest {
        //Given
        val comment = "please remove me"
        val action = RemoveComment(comment)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToRemoveComment(any(), comment)
        } returns ccStatus

        val responseFromModel = "There are 84 cornstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

}