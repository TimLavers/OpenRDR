package io.rippledown.model.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.model.KBInfo
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class KbFileDialogRequestTest {
    @Test
    fun `import request round trips with its id in a response`() {
        // Given
        val request = KbFileDialogRequest.Import("import-1")
        val response = ChatResponse("Choose an archive", kbFileDialogRequest = request)

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
        restored.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Import>().requestId shouldBe "import-1"
    }

    @Test
    fun `export request preserves both KB identity and display name`() {
        // Given
        val request = KbFileDialogRequest.Export("export-1", KBInfo("kb_1", "Thyroid Function"))
        val response = ChatResponse("Choose a destination", kbFileDialogRequest = request)

        // When
        val restored = Json.decodeFromString<ChatResponse>(Json.encodeToString(response))

        // Then
        restored shouldBe response
        val export = restored.kbFileDialogRequest.shouldBeInstanceOf<KbFileDialogRequest.Export>()
        export.requestId shouldBe "export-1"
        export.kbInfo.id shouldBe "kb_1"
        export.kbInfo.name shouldBe "Thyroid Function"
    }

    @Test
    fun `explicit null request is accepted`() {
        // Given
        val json = """{"text":"Hello","kbFileDialogRequest":null}"""

        // When
        val response = Json.decodeFromString<ChatResponse>(json)

        // Then
        response.kbFileDialogRequest shouldBe null
    }

    @Test
    fun `request id and export KB are required on the wire`() {
        // Given
        val invalidRequests = listOf(
            """{"type":"import"}""",
            """{"type":"export","requestId":"export-1"}"""
        )

        invalidRequests.forEach { json ->
            // When / Then
            shouldThrow<SerializationException> { Json.decodeFromString<KbFileDialogRequest>(json) }
        }
    }
}
