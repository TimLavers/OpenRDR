package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.SHOW_LAST_RULE_FOR_UNDO
import io.rippledown.constants.chat.UNDO_LAST_RULE
import io.rippledown.constants.chat.SYSTEM_ERROR_PREFIX
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.UndoRuleRefusedException
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.UserId
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.server.KBEndpoint
import io.rippledown.toJsonString
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class UndoConfirmationChatTest {
    @ParameterizedTest
    @ValueSource(strings = ["There are no rules to undo.", "Please review the most recent rule before undoing it."])
    fun `expected undo refusals are displayed as explanations`(message: String) = runTest {
        // Given
        val service = mockk<RuleService>()
        every { service.undoLastRuleSession() } throws UndoRuleRefusedException(message)
        val conversation = mockk<ConversationService>()
        coEvery { conversation.response(any<String>()) } returns ActionComment(UNDO_LAST_RULE).toJsonString()
        val manager = ChatManager(conversation, service, mockk())

        // When
        val response = manager.response("yes")

        // Then
        response.text shouldBe message
    }

    @Test
    fun `unexpected undo errors retain generic error handling`() = runTest {
        // Given
        val service = mockk<RuleService>()
        every { service.undoLastRuleSession() } throws IllegalStateException("Unexpected persistence failure")
        val conversation = mockk<ConversationService>()
        val action = ActionComment(UNDO_LAST_RULE).toJsonString()
        coEvery { conversation.response(any<String>()) } returns action
        val manager = ChatManager(conversation, service, mockk())

        // When
        val response = manager.response("yes")

        // Then
        response.text shouldBe "$SYSTEM_ERROR_PREFIX: '$action'"
    }

    @Test
    fun `undo refusal context is supplied for one turn and cleared by conversation reset`() {
        // Given
        val dialogue = RuleConversation(mockk())
        dialogue.undoRefused("The most recent rule changed since it was shown.")

        // When
        val retry = dialogue.prepareTurn("yes")
        val following = dialogue.prepareTurn("another question")
        dialogue.undoRefused("Please review the most recent rule before undoing it.")
        dialogue.reset()
        val restarted = dialogue.prepareTurn("hello")

        // Then
        retry.message shouldBe "[The server refused the undo: The most recent rule changed since it was shown.]\nyes"
        following.message shouldBe "another question"
        restarted.message shouldBe "hello"
    }

    @Test
    fun `chat displays a stale undo refusal and sends it to the model before a fresh preview`() = runTest {
        // Given
        val kb = KB(InMemoryKB(KBInfo("undo", "Undo")))
        val session = KBSession(kb, mockk())
        val alice = session.ruleSessionManagerFor(UserId("Alice"))
        val bob = session.ruleSessionManagerFor(UserId("Bob"))
        val case = RDRCaseBuilder().apply {
            addValue(kb.attributeManager.getOrCreate("Glucose"), defaultDate, "1")
        }.build("Case")
        session.locked {
            alice.startRuleSessionToAddComment(case, "First advice.")
            alice.commitCurrentRuleSession()
        }
        val conversation = mockk<ConversationService>()
        coEvery { conversation.response(any<String>()) } answers {
            val input = firstArg<String>()
            val action = if (input == "undo" || input.contains("The server refused the undo:"))
                SHOW_LAST_RULE_FOR_UNDO else UNDO_LAST_RULE
            ActionComment(action).toJsonString()
        }
        val manager = ChatManager(conversation, LockedRuleService(session, alice, KBEndpoint(session).coordinator), mockk())
        manager.response("undo")
        session.locked {
            kb.interpret(case)
            bob.startRuleSessionToAddComment(case, "Bob's advice.")
            bob.commitCurrentRuleSession()
        }

        // When
        val refusal = manager.response("yes")

        // Then the first confirmation never removes Bob's rule
        refusal.text shouldContain "The most recent rule changed since it was shown."
        refusal.text shouldContain "The most recent rule was added by Bob:"
        kb.ruleTree.size() shouldBe 3

        // When the next affirmation is contextualised for the model and only previews again
        val newPreview = manager.response("yes")

        // Then
        newPreview.text shouldContain "Bob's advice."
        newPreview.text shouldContain "Please confirm you wish to remove it."
        kb.ruleTree.size() shouldBe 3
        coVerify(exactly = 1) { conversation.response(match<String> {
            it.contains("The server refused the undo:") && it.contains(refusal.text) && it.endsWith("yes")
        }) }

        // When / Then a new confirmation finally undoes the rule just reviewed
        manager.response("yes").text shouldBe "rule undone"
        kb.ruleTree.size() shouldBe 2
    }
}
