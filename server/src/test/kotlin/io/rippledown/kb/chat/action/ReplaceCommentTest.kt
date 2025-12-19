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

class ReplaceCommentTest {
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
    fun `should start a rule session to replace a comment`() = runTest {
        val commentToRemove = "please remove me"
        val commentToAdd = "please add me"
        //Given
        val action = ReplaceComment(commentToRemove, commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToReplaceComment(any(), commentToRemove, commentToAdd)
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
    fun `should send CornerstoneStatus after starting a rule session to replace a comment`() = runTest {
        //Given
        val comment = "please replace me"
        val replacementComment = "please use me"
        val action = ReplaceComment(comment, replacementComment)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToReplaceComment(any(), comment, replacementComment)
        } returns ccStatus

        val responseFromModel = "There are 84 cornstone cases. Do you want to review them?"
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

}