package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.ASSIGN_DERIVED_VALUE
import io.rippledown.constants.chat.EXEMPT_CORNERSTONE
import io.rippledown.constants.chat.USER_ACTION
import io.rippledown.model.rule.CornerstoneStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RuleConversationTest {
    private val service = mockk<RuleService>()
    private val conversation = RuleConversation(service)

    @Test
    fun `a case less conversation needs neither rule context nor rule actions`() {
        // Given
        val caseLess = RuleConversation(null)

        // When
        val turn = caseLess.prepareTurn("hello")
        caseLess.rememberOffer(ActionComment(ASSIGN_DERIVED_VALUE, attributeName = "bmi", valueExpression = "1"))

        // Then
        turn shouldBe RuleConversation.Turn("hello", emptySet())
        caseLess.completeTurn(turn) shouldBe null
        caseLess.cornerstoneAction("allow") shouldBe null
        caseLess.assignmentAction("yes") shouldBe null
        caseLess.state shouldBe RuleConversation.State.Ready
    }

    @Test
    fun `allow requires both an active session and a pending cornerstone`() {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus(numberOfCornerstones = 1)

        // When
        val allowed = conversation.cornerstoneAction("allow")
        val genericYes = conversation.cornerstoneAction("yes")
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        val finished = conversation.cornerstoneAction("allow")
        every { service.isRuleSessionActive() } returns false
        val inactive = conversation.cornerstoneAction("allow")

        // Then
        allowed shouldBe ActionComment(EXEMPT_CORNERSTONE)
        genericYes shouldBe null
        finished shouldBe null
        inactive shouldBe null
    }

    @Test
    fun `the condition snapshot is independent of later mutations and preparing a retry keeps the question`() {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        val conditions = mutableSetOf("age is young")
        every { service.currentRuleSessionConditionTexts() } returns conditions
        val turn = conversation.prepareTurn("tear production is reduced")
        conditions.add("tear production is reduced")

        // When
        conversation.completeTurn(turn)
        val first = conversation.prepareTurn("yes")
        val retry = conversation.prepareTurn("yes")

        // Then
        turn.conditionsBefore shouldBe setOf("age is young")
        first shouldBe retry
        conversation.state shouldBe RuleConversation.State.AwaitingReasonReply
    }

    @Test
    fun `a failed or duplicate condition does not create a further reasons question`() {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
        val turn = conversation.prepareTurn("age is young")

        // When
        val response = conversation.completeTurn(turn)

        // Then
        response shouldBe null
        conversation.state shouldBe RuleConversation.State.Ready
    }

    @Test
    fun `ending a rule session stops including its question or asking for further reasons`() {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        every { service.currentRuleSessionConditionTexts() } returns emptySet()
        val turn = conversation.prepareTurn("age is young")
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
        conversation.completeTurn(turn)
        every { service.isRuleSessionActive() } returns false

        // When
        val next = conversation.prepareTurn("hello")
        val response = conversation.completeTurn(next)

        // Then
        next.message shouldBe "hello"
        response shouldBe null
        conversation.state shouldBe RuleConversation.State.Ready
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `an offer is consumed even when declined or blocked by an active session`(active: Boolean) {
        // Given
        every { service.isRuleSessionActive() } returns active
        every { service.offeredValueExpressionFor("bad") } returns "corrected"
        conversation.rememberOffer(ActionComment(ASSIGN_DERIVED_VALUE, attributeName = "bmi", valueExpression = "bad"))

        // When
        val action = conversation.assignmentAction(if (active) "yes" else "no")

        // Then
        action shouldBe null
        conversation.state shouldBe RuleConversation.State.Ready
        conversation.assignmentAction("yes") shouldBe null
    }

    @Test
    fun `unrelated or incomplete assignments do not invent an offer`() {
        // Given
        every { service.offeredValueExpressionFor("valid") } returns null
        val actions = listOf(
            ActionComment(USER_ACTION),
            ActionComment(ASSIGN_DERIVED_VALUE),
            ActionComment(ASSIGN_DERIVED_VALUE, attributeName = "bmi"),
            ActionComment(ASSIGN_DERIVED_VALUE, attributeName = "bmi", valueExpression = "valid")
        )

        // When
        actions.forEach(conversation::rememberOffer)

        // Then
        conversation.state shouldBe RuleConversation.State.Ready
        conversation.assignmentAction("yes") shouldBe null
    }

    @Test
    fun `a completed turn with a new condition asks for more and contextualises the next reply`() {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        every { service.currentRuleSessionConditionTexts() } returns emptySet()
        val turn = conversation.prepareTurn("age is young")
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")

        // When
        val action = conversation.completeTurn(turn)
        val next = conversation.prepareTurn("no")

        // Then
        action shouldBe ActionComment(USER_ACTION, message = RuleConversation.MORE_REASONS_QUESTION)
        conversation.state shouldBe RuleConversation.State.AwaitingReasonReply
        next.message shouldBe "[Current cornerstone status: Cornerstone: null, Index: -1, Total: 0]\n" +
                "[The server asked the user: ${RuleConversation.MORE_REASONS_QUESTION}]\nno"
        conversation.completeTurn(next) shouldBe null
        conversation.state shouldBe RuleConversation.State.Ready
    }

    @Test
    fun `an offered assignment is consumed by the next reply and reset removes stale offers`() {
        // Given
        every { service.isRuleSessionActive() } returns false
        every { service.offeredValueExpressionFor("bad formula") } returns "weight / height"
        val action = ActionComment(ASSIGN_DERIVED_VALUE, attributeName = "bmi", valueExpression = "bad formula")
        conversation.rememberOffer(action)

        // When
        val accepted = conversation.assignmentAction("yes")
        val again = conversation.assignmentAction("yes")
        conversation.rememberOffer(action)
        conversation.reset()

        // Then
        accepted shouldBe action.copy(valueExpression = "weight / height")
        again shouldBe null
        conversation.assignmentAction("yes") shouldBe null
        conversation.state shouldBe RuleConversation.State.Ready
    }
}
