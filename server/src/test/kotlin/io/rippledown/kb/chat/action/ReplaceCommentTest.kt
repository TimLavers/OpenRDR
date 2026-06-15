package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
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

        val responseFromModel = ChatResponse("There are 84 cornstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.summary()) }
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

        val responseFromModel = ChatResponse("There are 84 cornstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return error when rule session is already active`() = runTest {
        //Given
        val commentToRemove = "please replace me"
        val commentToAdd = "please use me"
        val action = ReplaceComment(commentToRemove, commentToAdd)
        coEvery { ruleService.isRuleSessionActive() } returns true

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToReplaceComment(any(), any(), any()) }
        coVerify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should start rule session when no rule session is active`() = runTest {
        //Given
        val commentToRemove = "please replace me"
        val commentToAdd = "please use me"
        val action = ReplaceComment(commentToRemove, commentToAdd)
        coEvery { ruleService.isRuleSessionActive() } returns false
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToReplaceComment(any(), commentToRemove, commentToAdd)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.startRuleSessionToReplaceComment(any(), commentToRemove, commentToAdd) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }

}