package io.rippledown.model.chat

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChatResponseTest {
    @Test
    fun `structured knowledge base listing round trips through JSON`() {
        // Given
        val response = ChatResponse(
            "Choose", listOf("Condition"), "Tip",
            KnowledgeBaseListing(listOf("Glucose", "Thyroids"), listOf("Zoo Animals"), "Thyroids")
        )

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
    }

    @Test
    fun `knowledge base listing defaults to absent`() {
        // Given
        val json = """{"text":"Hello"}"""

        // When
        val restored = Json.decodeFromString<ChatResponse>(json)

        // Then
        restored.kbListing shouldBe null
        restored shouldBe ChatResponse("Hello")
        ChatResponse("Hello").kbListing shouldBe null
    }

    @Test
    fun `listing supports no stored or open knowledge base`() {
        // Given
        val json = """{"text":"Choose","kbListing":{"storedNames":[],"demonstrationNames":["Zoo Animals"]}}"""

        // When
        val restored = Json.decodeFromString<ChatResponse>(json)

        // Then
        restored.kbListing shouldBe KnowledgeBaseListing(emptyList(), listOf("Zoo Animals"))
    }
}
