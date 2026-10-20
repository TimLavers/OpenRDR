package io.rippledown.integration.proxy

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GeminiCallCounterTest {

    @Test
    fun `each call line counts once and the token-count echo of a call does not`() {
        // Given
        val lines = listOf(
            "10:01 INFO Gemini - Gemini API call #1 (timeout=90000ms)",
            "10:01 INFO Conversation - Turn (API call #1) - tokens: prompt=10",
            "10:02 INFO Gemini - Gemini API call #2 (timeout=90000ms)",
            "10:02 INFO Gemini - Gemini API call #3 (timeout=90000ms)",
        )

        // When
        val calls = geminiCallsLogged(lines)

        // Then
        calls shouldBe 3
    }

    @Test
    fun `a log with no calls gives zero`() {
        // Given
        val lines = listOf("10:01 INFO Starting server")

        // When
        val calls = geminiCallsLogged(lines)

        // Then
        calls shouldBe 0
    }

    @Test
    fun `a server restarted mid-scenario appends to the log with its counter reset, and both runs count`() {
        // Given
        val lines = listOf(
            "Gemini API call #1 (timeout=90000ms)",
            "Gemini API call #2 (timeout=90000ms)",
            "Starting server",
            "Gemini API call #1 (timeout=90000ms)",
        )

        // When
        val calls = geminiCallsLogged(lines)

        // Then
        calls shouldBe 3
    }
}
