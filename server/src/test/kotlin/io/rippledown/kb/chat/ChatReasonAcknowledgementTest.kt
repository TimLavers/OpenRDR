package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.USER_ACTION
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ChatReasonAcknowledgementTest {
    @ParameterizedTest
    @ValueSource(strings = ["note", "commit", "malformed"])
    fun `the server acknowledgement retains the previous phrase note`(modelReply: String) = runTest {
        // Given a stored condition reused under a new phrase during the model turn
        val service = mockk<RuleService>()
        val conversation = mockk<ConversationService>()
        val acknowledgements = ReasonAcknowledgements()
        val manager = ChatManager(conversation, service, mockk(), reasonAcknowledgements = acknowledgements)
        val case = RDRCase(CaseId(1L, "GlucoseCase"))
        val transformer = KBReasonTransformer(case, service, manager, acknowledgements)
        val condition = greaterThanOrEqualTo(7, Attribute(1, "Glucose"), 11.0)
            .copy(userExpression = "elevated glucose")
        every { service.isRuleSessionActive() } returns true
        every { service.cornerstoneStatus() } returns CornerstoneStatus()
        every { service.currentRuleSessionConditionTexts() } returns emptySet()
        every { service.conditionForExpression(case, "raised glucose") } returns
                ConditionParsingResult(condition, expression = "raised glucose")
        val note = "Added your reason '${condition.asText()}' (you previously called this 'elevated glucose')."
        coEvery { conversation.response(any()) } coAnswers {
            transformer.transform("raised glucose")
            every { service.currentRuleSessionConditionTexts() } returns setOf(condition.asText())
            when (modelReply) {
                "note" -> ActionComment(
                    USER_ACTION,
                    message = "$note ${RuleConversation.MORE_REASONS_QUESTION}"
                ).toJsonString()

                "commit" -> """{"action":"CommitRule"}"""
                else -> "{invalid JSON"
            }
        }

        // When
        val response = manager.response("raised glucose")

        // Then the server keeps the note and still asks its own follow-up question
        response.text shouldBe "$note\n\n${RuleConversation.MORE_REASONS_QUESTION}"
        coVerify(exactly = 0) { service.commitCurrentRuleSession() }

        // When the next turn adds a different condition without a transformation message
        coEvery { conversation.response(any()) } coAnswers {
            every { service.currentRuleSessionConditionTexts() } returns setOf(condition.asText(), "Age is high")
            "Allow the change?"
        }
        val next = manager.response("Age is high")

        // Then the earlier phrase note does not leak into the next acknowledgement
        next.text shouldBe "Added:\nAge is high\n\n${RuleConversation.MORE_REASONS_QUESTION}"
        acknowledgements.snapshot() shouldBe emptyMap()
    }

    @Test
    fun `messages are cleared when a new conversation starts`() = runTest {
        // Given
        val acknowledgements = ReasonAcknowledgements()
        val condition = greaterThanOrEqualTo(7, Attribute(1, "Glucose"), 11.0)
            .copy(userExpression = "elevated glucose")
        acknowledgements.record(ConditionParsingResult(condition, expression = "raised glucose"))
        val conversation = mockk<ConversationService>()
        coEvery { conversation.startConversation() } returns "Hello"
        val manager = ChatManager(conversation, null, mockk(), reasonAcknowledgements = acknowledgements)

        // When
        manager.startConversation(null)

        // Then
        acknowledgements.snapshot() shouldBe emptyMap()
    }
}
