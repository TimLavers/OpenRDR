package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AssignDerivedValueTest {
    private lateinit var ruleService: RuleService
    private lateinit var currentCase: ViewableCase
    private lateinit var modelResponder: ModelResponder

    @BeforeTest
    fun setup() {
        ruleService = mockk()
        currentCase = mockk()
        modelResponder = mockk()
        every { currentCase.derivedValues() } returns emptyList()
    }

    @Test
    fun `should start a rule session to assign a derived value`() = runTest {
        //Given
        val action = AssignDerivedValue("Diabetes status", "\"diabetic\"")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        coEvery {
            ruleService.startRuleSessionToAssignValue(any(), "Diabetes status", "\"diabetic\"")
        } returns ccStatus

        val responseFromModel = ChatResponse("There is 1 cornerstone case. Do you want to review it?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { modelResponder.response(ccStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send cornerstone status after starting a derived value session`() = runTest {
        //Given
        val action = AssignDerivedValue("Diabetes status", "\"diabetic\"")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        coEvery {
            ruleService.startRuleSessionToAssignValue(any(), any(), any())
        } returns ccStatus

        val responseFromModel = ChatResponse("There is 1 cornerstone case.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        action.doIt(ruleService, currentCase, modelResponder)

        //Then
        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return error when rule session is already active`() = runTest {
        //Given
        val action = AssignDerivedValue("Diabetes status", "\"diabetic\"")
        every { ruleService.isRuleSessionActive() } returns true

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToAssignValue(any(), any(), any()) }
        coVerify(exactly = 0) { ruleService.sendCornerstoneStatus() }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should relay a refusal message from the rule service`() = runTest {
        //Given
        val action = AssignDerivedValue("Glucose", "\"high\"")
        every { ruleService.isRuleSessionActive() } returns false
        coEvery {
            ruleService.startRuleSessionToAssignValue(any(), any(), any())
        } throws IllegalStateException("The name \"Glucose\" is already used by an externally supplied attribute. Please choose a different name.")

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response.text shouldBe "The name \"Glucose\" is already used by an externally supplied attribute. Please choose a different name."
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should refuse to assign a derived value when the name already exists ignoring case`() = runTest {
        //Given
        val action = AssignDerivedValue("BMI", "weight / (height * height)")
        every { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("BMI") } returns Attribute(1, "bmi", AttributeKind.DERIVED)

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response.text shouldBe nameClashWithExistingDerivedAttributeMessage("BMI")
        coVerify(exactly = 0) { ruleService.startRuleSessionToAssignValue(any(), any(), any()) }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should ask whether to replace a value the case already has`() = runTest {
        //Given the attribute already has a value for this case
        val action = AssignDerivedValue("BMI", "weight / height ^ 3")
        every { ruleService.isRuleSessionActive() } returns false
        every { currentCase.derivedValues() } returns listOf(
            DerivedValueInfo("BMI", "30.93", "weight / height ^ 2", emptyList())
        )

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then the user is told and asked, rather than a change being assumed
        response.text shouldBe alreadyAssignedForCaseMessage("BMI", "30.93", "weight / height ^ 3")
        coVerify(exactly = 0) { ruleService.startRuleSessionToAssignValue(any(), any(), any()) }
        coVerify(exactly = 0) { ruleService.startRuleSessionToReplaceAssignment(any(), any(), any()) }
        coVerify(exactly = 0) { modelResponder.response(any<String>()) }
    }

    @Test
    fun `should ask about an existing value ignoring case in the name`() = runTest {
        //Given the case has the value under a differently cased name
        val action = AssignDerivedValue("bmi", "weight / height ^ 3")
        every { ruleService.isRuleSessionActive() } returns false
        every { currentCase.derivedValues() } returns listOf(
            DerivedValueInfo("BMI", "30.93", "weight / height ^ 2", emptyList())
        )

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then the question names the attribute as the case has it
        response.text shouldBe alreadyAssignedForCaseMessage("BMI", "30.93", "weight / height ^ 3")
        coVerify(exactly = 0) { ruleService.startRuleSessionToAssignValue(any(), any(), any()) }
    }

    @Test
    fun `should assign when another derived attribute has a value but this one does not`() = runTest {
        //Given the case has a different derived value
        val action = AssignDerivedValue("BMI", "weight / height ^ 2")
        every { ruleService.isRuleSessionActive() } returns false
        every { ruleService.attributeForName("BMI") } returns null
        every { currentCase.derivedValues() } returns listOf(
            DerivedValueInfo("Risk", "high", "\"high\"", emptyList())
        )
        val ccStatus = CornerstoneStatus()
        coEvery { ruleService.startRuleSessionToAssignValue(any(), any(), any()) } returns ccStatus
        val responseFromModel = ChatResponse("Why?")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then the session starts as normal
        response shouldBe responseFromModel
        coVerify { ruleService.startRuleSessionToAssignValue(any(), "BMI", "weight / height ^ 2") }
    }
}
