package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ChatCommentVariable
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.Attribute
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
        val commentToAdd = "Patient {Name} has glucose {Glucose}"
        val internalComment = "Patient \${} has glucose \${}"
        val variables =
            listOf(ChatCommentVariable(attributeName = "Name"), ChatCommentVariable(attributeName = "Glucose"))
        val resolvedVariables = listOf(CommentVariable(1), CommentVariable(2))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("Name") } returns Attribute(1, "Name")
        every { ruleService.attributeForName("Glucose") } returns Attribute(2, "Glucose")
        coEvery {
            ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables)
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
        val commentToAdd = "Patient {Name} has glucose {Glucose}"
        val internalComment = "Patient \${} has glucose \${}"
        val variables =
            listOf(ChatCommentVariable(attributeName = "Name"), ChatCommentVariable(attributeName = "Glucose"))
        val resolvedVariables = listOf(CommentVariable(1), CommentVariable(2))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("Name") } returns Attribute(1, "Name")
        every { ruleService.attributeForName("Glucose") } returns Attribute(2, "Glucose")
        coEvery {
            ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should ignore variables when the comment has no placeholders`() = runTest {
        //Given - the model occasionally attaches a variable to a comment that merely mentions an
        // attribute name but contains no {placeholder}; such variables must be dropped.
        val commentToAdd = "CDE is also normal."
        val variables = listOf(ChatCommentVariable(attributeName = "CDE"))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery { ruleService.isRuleSessionActive() } returns false
        coEvery {
            ruleService.startRuleSessionToAddComment(currentCase, commentToAdd, emptyList())
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then - the comment is unchanged and no variable is attached
        coVerify { ruleService.startRuleSessionToAddComment(currentCase, commentToAdd, emptyList()) }
        coVerify(exactly = 0) { ruleService.attributeForName(any()) }
    }

    @Test
    fun `should ignore variables in excess of the placeholders present`() = runTest {
        //Given - one placeholder but two variables supplied; only the first is kept.
        val commentToAdd = "Patient {Name} is well."
        val internalComment = "Patient \${} is well."
        val variables =
            listOf(ChatCommentVariable(attributeName = "Name"), ChatCommentVariable(attributeName = "Glucose"))
        val resolvedVariables = listOf(CommentVariable(1))
        val action = AddComment(commentToAdd, variables)
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("Name") } returns Attribute(1, "Name")
        coEvery {
            ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables) }
    }

    @Test
    fun `should return error when rule session is already active for AddComment with variables`() = runTest {
        //Given
        val commentToAdd = "Patient {Name} has glucose {Glucose}"
        val variables =
            listOf(ChatCommentVariable(attributeName = "Name"), ChatCommentVariable(attributeName = "Glucose"))
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
        val commentToAdd = "Patient {Name} has glucose {Glucose}"
        val internalComment = "Patient \${} has glucose \${}"
        val variables =
            listOf(ChatCommentVariable(attributeName = "Name"), ChatCommentVariable(attributeName = "Glucose"))
        val resolvedVariables = listOf(CommentVariable(1), CommentVariable(2))
        val action = AddComment(commentToAdd, variables)
        coEvery { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("Name") } returns Attribute(1, "Name")
        every { ruleService.attributeForName("Glucose") } returns Attribute(2, "Glucose")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 42, numberOfCornerstones = 84)
        coEvery {
            ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables)
        } returns ccStatus

        val responseFromModel = ChatResponse("There are 84 cornerstone cases. Do you want to review them?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.startRuleSessionToAddComment(currentCase, internalComment, resolvedVariables) }
        coVerify { ruleService.sendCornerstoneStatus() }
        response shouldBe responseFromModel
    }
}