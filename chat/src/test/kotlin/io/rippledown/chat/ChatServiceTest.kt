package io.rippledown.chat

import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class ChatServiceTest {

    @Test
    fun `should start a chat with Gemini`() {
        //Given
        val geminiChatService = GeminiChatService("", emptyList())

        //When
        val chat = geminiChatService.startChat()
        val prompt = "What is the smallest prime number?"
        val response = chat.sendMessage(prompt)

        //Then
        response.text() shouldContain "2"
    }

    @Test
    fun `should start a chat with Gemini with system instruction`() {
        //Given
        val geminiChatService = GeminiChatService(
            "Format the response as a json object with key 'answer' and numeric value only.", emptyList()
        )

        //When
        val chat = geminiChatService.startChat()
        val prompt = "What is the smallest prime number?"
        val response = chat.sendMessage(prompt)

        //Then
        response.text() shouldContain "\"answer\": 2"
    }
}