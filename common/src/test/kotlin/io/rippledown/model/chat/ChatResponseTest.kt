package io.rippledown.model.chat

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChatResponseTest {
    @Test
    fun `knowledge base choices round trip through JSON`() {
        // Given
        val response = ChatResponse("Choose", listOf("Condition"), "Tip", listOf("Glucose", "Zoo Animals"))

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
    }

    @Test
    fun `knowledge base choices default to empty`() {
        // Given
        val json = """{"text":"Hello"}"""

        // When
        val restored = Json.decodeFromString<ChatResponse>(json)

        // Then
        restored.kbChoices shouldBe emptyList()
        restored shouldBe ChatResponse("Hello")
        ChatResponse("Hello").kbChoices shouldBe emptyList()
    }
}
