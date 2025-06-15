package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.type.content
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GeminiServiceTest {

    @Test
    fun `should start a chat with Gemini`() = runTest {
        //Given
        val geminiChatService = GeminiChatService("", emptyList())

        //When
        val chat = geminiChatService.startChat()
        val prompt = "What is the smallest prime number?"
        val response = chat.sendMessage(
            content {
                text(prompt)
            }
        )

        //Then
        response.text shouldContain "2"
    }

    @Test
    fun `should start a chat with Gemini with system instruction`() = runTest {
        //Given
        val geminiChatService = GeminiChatService(
            "Format the response as a json object with key 'answer' and numeric value only.", emptyList()
        )

        //When
        val chat = geminiChatService.startChat()
        val prompt = "What is the smallest prime number?"
        val response = chat.sendMessage(
            content {
                text(prompt)
            }
        )

        //Then
        response.text shouldContain "\"answer\": 2"
    }
}