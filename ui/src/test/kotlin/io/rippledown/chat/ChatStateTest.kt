package io.rippledown.chat

import io.kotest.matchers.shouldBe
import io.rippledown.model.UserId
import io.rippledown.model.chat.ChatResponse
import org.junit.jupiter.api.Test

class ChatStateTest {
    @Test
    fun `a notice is appended without ending the pending chat response`() {
        // Given
        val state = ChatState()
        state.userMessage(UserMessage("What changed?"))

        // When
        state.notice("Bob: Added a rule.")

        // Then
        state.history shouldBe listOf(UserMessage("What changed?"), NoticeMessage("Bob: Added a rule."))
        state.history.last().isUser shouldBe false
        state.awaitingResponse shouldBe true
    }

    @Test
    fun `a notice is not considered the most recent bot response`() {
        // Given
        val state = ChatState()
        state.receive(ChatResponse("Welcome.")) {}
        val user = UserId("notice-test")

        // When
        state.notice("Bob: Added a rule.")
        ChatTestHook.update(user, state.history, true)

        // Then
        ChatTestHook.snapshot(user).mostRecentBotText shouldBe "Welcome."
        ChatTestHook.snapshot(user).messageList.last() shouldBe NoticeMessage("Bob: Added a rule.")
        state.awaitingResponse shouldBe false
    }

    @Test
    fun `a bot response matching a notice is still received`() {
        // Given
        val state = ChatState()
        state.notice("Changed.")

        // When
        state.receive(ChatResponse("Changed.")) {}

        // Then
        state.history shouldBe listOf(NoticeMessage("Changed."), BotMessage("Changed."))
    }
}
