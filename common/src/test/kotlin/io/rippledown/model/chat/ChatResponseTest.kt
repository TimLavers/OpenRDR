package io.rippledown.model.chat

import io.kotest.matchers.shouldBe
import io.rippledown.model.rule.AiBooleanExample
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChatResponseTest {
    @Test
    fun `capability sections round trip and old responses have no sections`() {
        // Given
        val response = ChatResponse(
            "Help",
            capabilities = listOf(CapabilitySection("Knowledge bases", listOf("Import", "Export")))
        )

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
        Json.decodeFromString<ChatResponse>("""{"text":"Hello"}""").capabilities shouldBe emptyList()
    }

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
        restored.kbFileDialogRequest shouldBe null
        restored shouldBe ChatResponse("Hello")
        ChatResponse("Hello").kbListing shouldBe null
        Json.encodeToString(ChatResponse("Hello")) shouldBe json
    }

    @Test
    fun `AI definition draft view round trips and defaults to absent`() {
        // Given
        val draft = AiDefinitionDraftView(
            attributeName = "hasDiabetes", source = "clinical_notes", criteria = "Personal history",
            examples = listOf(AiBooleanExample("T2 Diab", true)),
            previewValue = "true", evidence = "T2 Diab", prompt = "Accept this definition?"
        )
        val response = ChatResponse("Draft offer", draft = draft)

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
        restored.draft?.previewText shouldBe "Preview: true\nEvidence: \"T2 Diab\""
        Json.decodeFromString<ChatResponse>("""{"text":"Hello"}""").draft shouldBe null
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
