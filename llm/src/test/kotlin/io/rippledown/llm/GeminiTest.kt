package io.rippledown.llm

import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GeminiTest {
    @Test
    fun `should call Gemini`() {
        val chat = geminiClient.chats.create(GEMINI_MODEL, generateContentConfig("You are a helpful assistant."))
        chat.sendMessage("Highest mountain in the world?").text() shouldContain "Everest"
    }
}