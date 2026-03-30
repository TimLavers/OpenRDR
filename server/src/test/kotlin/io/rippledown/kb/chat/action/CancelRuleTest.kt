package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.CancelRule.Companion.RULE_CANCELLED_MESSAGE
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class CancelRuleTest {
    private lateinit var ruleService: RuleService

    @BeforeTest
    fun setup() {
        ruleService = mockk()
    }

    @Test
    fun `should cancel the current rule session`() = runTest {
        val action = CancelRule()

        action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        coVerify { ruleService.cancelCurrentRuleSession() }
    }

    @Test
    fun `should send a rule session completed message after cancelling`() = runTest {
        val action = CancelRule()

        action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        coVerify { ruleService.sendRuleSessionCompleted() }
    }

    @Test
    fun `should return a rule cancelled message`() = runTest {
        val action = CancelRule()

        val response = action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        response shouldBe ChatResponse(RULE_CANCELLED_MESSAGE)
    }

    @Test
    fun `should cancel before sending rule session completed`() = runTest {
        val action = CancelRule()

        action.doIt(ruleService, mockk<ViewableCase>(), mockk<ModelResponder>())

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            ruleService.cancelCurrentRuleSession()
            ruleService.sendRuleSessionCompleted()
        }
    }
}
