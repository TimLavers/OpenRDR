package io.rippledown.integration.files

import io.kotest.matchers.shouldBe
import io.rippledown.chat.*
import io.rippledown.constants.chat.CHAT_BOT_NO_RESPONSE_MESSAGE
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KbFileDialogRequest
import io.rippledown.model.chat.KnowledgeBaseListing
import org.junit.jupiter.api.Test

class ChatStateTest {
    @Test
    fun `empty response displays the fallback but still dispatches a file request`() {
        // Given
        val state = ChatState()
        val request = KbFileDialogRequest.Import("one")
        val requests = mutableListOf<KbFileDialogRequest>()

        // When
        state.receive(ChatResponse("", kbFileDialogRequest = request)) { requests.add(it) }

        // Then
        state.history shouldBe listOf(BotMessage(CHAT_BOT_NO_RESPONSE_MESSAGE))
        requests shouldBe listOf(request)
    }

    @Test
    fun `listing renders once and a changed listing with the same prose is still shown`() {
        // Given
        val state = ChatState()
        val listing = KnowledgeBaseListing(storedNames = listOf("Clinic"), demonstrationNames = emptyList())
        val updated = listing.copy(openName = "Clinic")

        // When
        state.receive(ChatResponse("Knowledge bases", kbListing = listing)) {}
        state.receive(ChatResponse("Knowledge bases", kbListing = listing)) {}
        state.receive(ChatResponse("Knowledge bases", kbListing = updated)) {}

        // Then
        state.history shouldBe listOf(
            KbChoiceListMessage("Knowledge bases", listing), KbChoiceListMessage("Knowledge bases", updated)
        )
    }

    @Test
    fun `listing is not hidden by an earlier text message with identical prose`() {
        // Given
        val state = ChatState()
        val listing = KnowledgeBaseListing(storedNames = emptyList(), demonstrationNames = listOf("Zoo Animals"))
        state.receive(ChatResponse("Knowledge bases")) {}

        // When
        state.receive(ChatResponse("Knowledge bases", kbListing = listing)) {}

        // Then
        state.history shouldBe listOf(BotMessage("Knowledge bases"), KbChoiceListMessage("Knowledge bases", listing))
    }

    @Test
    fun `file requests are delivered independently of display text deduplication`() {
        // Given
        val state = ChatState()
        val requests = mutableListOf<KbFileDialogRequest>()
        val first = KbFileDialogRequest.Import("first")
        val second = KbFileDialogRequest.Import("second")

        // When
        state.receive(ChatResponse("Choose a file", kbFileDialogRequest = first)) { requests.add(it) }
        state.receive(ChatResponse("Choose a file", kbFileDialogRequest = second)) { requests.add(it) }

        // Then
        state.history shouldBe listOf(BotMessage("Choose a file"))
        requests shouldBe listOf(first, second)
    }

    @Test
    fun `local completion survives the next conversation greeting and is never sent as user input`() {
        // Given
        val state = ChatState()
        state.userMessage(UserMessage("Import a KB"))

        // When
        state.localMessage("Imported \"Clinic\" and opened it.")
        state.receive(ChatResponse("Would you like to add a comment?")) {}

        // Then
        state.history shouldBe listOf(
            UserMessage("Import a KB"),
            BotMessage("Imported \"Clinic\" and opened it."),
            BotMessage("Would you like to add a comment?")
        )
        state.awaitingResponse shouldBe false
    }

    @Test
    fun `user submission waits for a response without adding empty messages`() {
        // Given
        val state = ChatState()

        // When
        state.userMessage(UserMessage(""))

        // Then
        state.awaitingResponse shouldBe true
        state.history shouldBe emptyList()
    }

    @Test
    fun `tip and suggestions retain their rendering order`() {
        // Given
        val state = ChatState()

        // When
        state.receive(ChatResponse("Choose", tip = "Tip", suggestions = listOf("A", "B"))) {}

        // Then
        state.history shouldBe listOf(TipMessage("Tip"), BotMessage("Choose"), SuggestionListMessage(listOf("A", "B")))
    }
}
