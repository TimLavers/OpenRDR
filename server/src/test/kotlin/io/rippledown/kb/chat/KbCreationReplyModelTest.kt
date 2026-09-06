package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.rippledown.chat.Conversation
import io.rippledown.constants.chat.LIST_KNOWLEDGE_BASES
import io.rippledown.constants.chat.NAME_THE_NEW_KB
import io.rippledown.constants.chat.noKbGreeting
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/** Language examples against the configured model; workflow transitions are tested without a model separately. */
@EnabledIfEnvironmentVariable(named = "API_KEY", matches = ".+")
class KbCreationReplyModelTest {
    private fun conversation() = Conversation(
        KBChatService.createKBChatService(null, null, emptyList()), emptyMap(), openingMessage = null
    )

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "oui | CONFIRM",
            "sí | CONFIRM",
            "go ahead | CONFIRM",
            "non merci | DENY",
            "not now | DENY",
            "maybe | UNCLEAR",
            "What is a knowledge base? | UNCLEAR",
            "List the knowledge bases | OTHER_REQUEST"
        ]
    )
    fun `interprets a reply to the actual startup question`(message: String, intent: KbCreationIntent) = runTest {
        // Given
        val conversation = conversation()
        conversation.startConversation()
        val interpreter = KbCreationReplyInterpreter(conversation)

        // When
        val reply = interpreter.interpret(KbCreationStage.OFFER_CREATION, noKbGreeting(emptyList()), message)

        // Then
        reply shouldBe KbCreationReply(intent)
    }

    @Test
    fun `extracts an untranslated name from an implicit creation request`() = runTest {
        // Given
        val conversation = conversation()
        conversation.startConversation()
        val interpreter = KbCreationReplyInterpreter(conversation)

        // When
        val reply = interpreter.interpret(
            KbCreationStage.OFFER_CREATION, noKbGreeting(emptyList()), "Crée une base appelée Thyroïde"
        )

        // Then
        reply shouldBe KbCreationReply(KbCreationIntent.CONFIRM_WITH_NAME, "Thyroïde")
    }

    @Test
    fun `interprets successive server questions and returns to ordinary actions after the workflow`() = runTest {
        // Given
        val conversation = conversation()
        conversation.startConversation()
        val interpreter = KbCreationReplyInterpreter(conversation)

        // When
        val confirmed = interpreter.interpret(KbCreationStage.OFFER_CREATION, noKbGreeting(emptyList()), "oui")
        val named = interpreter.interpret(KbCreationStage.AWAITING_NAME, NAME_THE_NEW_KB, "Call it Coogee Beach")
        val cancelled = interpreter.interpret(KbCreationStage.AWAITING_NAME, NAME_THE_NEW_KB, "Actually, no")
        val ordinaryReply = conversation.response("List the knowledge bases")

        // Then
        confirmed shouldBe KbCreationReply(KbCreationIntent.CONFIRM)
        named shouldBe KbCreationReply(KbCreationIntent.CONFIRM_WITH_NAME, "Coogee Beach")
        cancelled shouldBe KbCreationReply(KbCreationIntent.DENY)
        extractJsonFragments(ordinaryReply).single()
            .fromJsonString<ActionComment>().action shouldBe LIST_KNOWLEDGE_BASES
    }
}
