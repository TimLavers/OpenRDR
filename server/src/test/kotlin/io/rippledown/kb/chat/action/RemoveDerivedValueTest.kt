package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

class RemoveDerivedValueTest {
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
    fun `should start a rule session to remove a derived value`() = runTest {
        val action = RemoveDerivedValue("Diabetes status")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        coEvery { ruleService.startRuleSessionToRemoveAssignment(any(), "Diabetes status") } returns ccStatus
        val responseFromModel = ChatResponse("There is 1 cornerstone case.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        val response = action.doIt(ruleService, currentCase, modelResponder)

        coVerify { modelResponder.response(ccStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should send cornerstone status after removing a derived value`() = runTest {
        val action = RemoveDerivedValue("Diabetes status")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        coEvery { ruleService.startRuleSessionToRemoveAssignment(any(), any()) } returns ccStatus
        val responseFromModel = ChatResponse("There is 1 cornerstone case.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        action.doIt(ruleService, currentCase, modelResponder)

        coVerify { ruleService.sendCornerstoneStatus() }
    }

    @Test
    fun `should return error when rule session is already active`() = runTest {
        val action = RemoveDerivedValue("Diabetes status")
        every { ruleService.isRuleSessionActive() } returns true

        val response = action.doIt(ruleService, currentCase, modelResponder)

        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToRemoveAssignment(any(), any()) }
    }
}
