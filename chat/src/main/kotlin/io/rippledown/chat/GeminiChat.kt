package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.chat.service.ChatService

suspend fun responseFor(message: String, systemInstruction: String): String {
    val chatService = ChatService(systemInstruction)
    val chat = chatService.startChat(
        history = listOf(
            content(role = "model") { text("Great to meet you. What would you like to know?") },
        ),
    )
    val response = chat.sendMessage(content { text(message) })
    return response.text ?: ""

}