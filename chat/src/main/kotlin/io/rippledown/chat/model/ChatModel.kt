package io.rippledown.chat.model

import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.chat.service.ChatService
import io.rippledown.constants.chat.CHAT_BOT_INITIAL_MESSAGE
import kotlinx.coroutines.runBlocking

class ChatModel(aiService: ChatService) {

    private val chat = aiService.startChat(
        history = listOf(
            content(role = "model") { text(CHAT_BOT_INITIAL_MESSAGE) },
        ),
    )

    fun responseFor(userMessage: String) {
        val response = runBlocking {
            chat.sendMessage(userMessage)
        }

        println("ChatModel: sendMessage: $userMessage")
        println("ChatModel: response:    ${response.text}")
    }

}