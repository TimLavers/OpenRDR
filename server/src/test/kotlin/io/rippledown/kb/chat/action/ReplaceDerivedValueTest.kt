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

class ReplaceDerivedValueTest {
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
    fun `should start a rule session to replace a derived value`() = runTest {
        val action = ReplaceDerivedValue("Diabetes status", "\"severely diabetic\"")
        val ccStatus = CornerstoneStatus(indexOfCornerstoneToReview = 0, numberOfCornerstones = 1)
        coEvery {
            ruleService.startRuleSessionToReplaceAssignment(any(), "Diabetes status", "\"severely diabetic\"")
        } returns ccStatus
        val responseFromModel = ChatResponse("There is 1 cornerstone case.")
        coEvery { modelResponder.response(any<String>()) } returns responseFromModel

        val response = action.doIt(ruleService, currentCase, modelResponder)

        coVerify { modelResponder.response(ccStatus.summary()) }
        response shouldBe responseFromModel
    }

    @Test
    fun `should return error when rule session is already active`() = runTest {
        val action = ReplaceDerivedValue("Diabetes status", "\"severely diabetic\"")
        every { ruleService.isRuleSessionActive() } returns true

        val response = action.doIt(ruleService, currentCase, modelResponder)

        response shouldBe ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        coVerify(exactly = 0) { ruleService.startRuleSessionToReplaceAssignment(any(), any(), any()) }
    }
}
