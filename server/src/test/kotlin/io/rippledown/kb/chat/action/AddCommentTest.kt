package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.CommentVariable
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AddCommentTest {
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
    fun `should start a rule session to add a comment`() = runTest {
        //Given
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, emptyList())
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.summary()) }
        response shouldBe responseFromModel
    }
    @Test
    fun `should send CornerstoneStatus after starting a rule session to add a comment`() = runTest {
        //Given
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, emptyList())
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
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        coEvery { ruleService.isRuleSessionActive() } returns true

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToAddComment(any(), any()) }
        coVerify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should start rule session when no rule session is active`() = runTest {
        //Given
        val commentToAdd = "please add me"
        val action = AddComment(commentToAdd)
        coEvery { ruleService.isRuleSessionActive() } returns false
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, emptyList())
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.startRuleSessionToAddComment(any(), commentToAdd, emptyList()) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }

    @Test
    fun `should start a rule session to add a comment with variables`() = runTest {
        //Given
        val commentToAdd = "Patient ${'$'}{} has glucose ${'$'}{}"
        val variables = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, variables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send CornerstoneStatus after starting a rule session to add a comment with variables`() = runTest {
        //Given
        val commentToAdd = "Patient ${'$'}{} has glucose ${'$'}{}"
        val variables = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, variables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return error when rule session is already active for AddComment with variables`() = runTest {
        //Given
        val commentToAdd = "Patient ${'$'}{} has glucose ${'$'}{}"
        val variables = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val action = AddComment(commentToAdd, variables)
        coEvery { ruleService.isRuleSessionActive() } returns true

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToAddComment(any(), any(), any()) }
        coVerify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should start rule session when no rule session is active for AddComment with variables`() = runTest {
        //Given
        val commentToAdd = "Patient ${'$'}{} has glucose ${'$'}{}"
        val variables = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val action = AddComment(commentToAdd, variables)
        coEvery { ruleService.isRuleSessionActive() } returns false
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(any(), commentToAdd, variables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.startRuleSessionToAddComment(any(), commentToAdd, variables) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}