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
}
