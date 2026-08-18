package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class EditDerivedAttributeDefinitionTest {
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
    fun `should edit the definition and relay the summary`() = runTest {
        //Given
        val action = EditDerivedAttributeDefinition("BMI", "weight / (height * height)")
        every { ruleService.isRuleSessionActive() } returns false
        val summary = "Changed the definition of \"BMI\" from weight / height to weight / (height * height)."
        every {
            ruleService.editDerivedAttributeDefinition("BMI", "weight / (height * height)")
        } returns summary

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(summary)
    }

    @Test
    fun `should return error when rule session is already active`() = runTest {
        //Given
        val action = EditDerivedAttributeDefinition("BMI", "weight / (height * height)")
        every { ruleService.isRuleSessionActive() } returns true

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.editDerivedAttributeDefinition(any(), any()) }
    }

    @Test
    fun `should relay a refusal message from the rule service`() = runTest {
        //Given
        val action = EditDerivedAttributeDefinition("BMI", "BMI * 2")
        every { ruleService.isRuleSessionActive() } returns false
        val refusal = "This definition cannot be used: it would make \"BMI\" depend on itself (BMI → BMI)."
        every {
            ruleService.editDerivedAttributeDefinition(any(), any())
        } throws IllegalStateException(refusal)

        //When
        val response = action.doIt(ruleService, currentCase, modelResponder)

        //Then
        response.text shouldBe refusal
    }
}
