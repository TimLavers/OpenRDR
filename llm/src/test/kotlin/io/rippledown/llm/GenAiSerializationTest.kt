package io.rippledown.llm

import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Type
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Exercises the SDK's Jackson integration without making a provider request. */
class GenAiSerializationTest {
    @Test
    fun `structured request configuration survives JSON serialization`() {
        // Given a request using the optional and nested types sent to Gemini
        val config = GenerateContentConfig.builder()
            .temperature(0.0f)
            .systemInstruction(Content.fromParts(Part.fromText("Classify the notes.")))
            .responseMimeType("application/json")
            .responseSchema(Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(mapOf("value" to Schema.builder().type(Type.Known.BOOLEAN).build()))
                .required("value")
                .build())
            .build()

        // When the SDK uses the resolved Jackson modules to round-trip the request
        val restored = GenerateContentConfig.fromJson(config.toJson())

        // Then configuration and schema remain intact
        restored shouldBe config
    }

    @Test
    fun `structured response preserves candidate text and metadata`() {
        // Given a provider response with a nested structured result
        val json = """{
            "candidates": [{
                "content": {"role": "model", "parts": [{"text": "{\"value\":true,\"evidence\":null}"}]},
                "finishReason": "STOP"
            }],
            "modelVersion": "test-model"
        }"""

        // When the SDK deserializes it using the resolved Jackson modules
        val response = GenerateContentResponse.fromJson(json)
        val candidate = response.candidates().orElseThrow().single()

        // Then the fields consumed by the Boolean classifier are available
        candidate.content().orElseThrow().parts().orElseThrow().single().text().orElseThrow() shouldBe
            """{"value":true,"evidence":null}"""
        candidate.finishReason().orElseThrow().toString() shouldBe "STOP"
        response.modelVersion().orElseThrow() shouldBe "test-model"
    }
}
