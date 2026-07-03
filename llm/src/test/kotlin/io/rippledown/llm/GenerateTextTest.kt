package io.rippledown.llm

import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.beEmpty
import kotlin.test.Test

class GenerateTextTest {
    @Test
    fun `generateText should return non-empty response`() {
        // This test requires a valid API key to run
        // It's marked as a manual test or should be skipped in CI without credentials
        if (GEMINI_API_KEY.isBlank()) return

        val result = generateText(
            systemInstruction = "You are a helpful assistant. Answer briefly.",
            userContent = "What is 2+2?"
        )
        result shouldNotBe
                beEmpty()
    }
}
