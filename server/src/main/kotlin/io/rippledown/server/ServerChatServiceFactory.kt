package io.rippledown.server

import io.rippledown.chat.ChatService
import io.rippledown.chat.GeminiChatService

class ServerChatServiceFactory{
    fun createChatService(): ChatService {
        val systemPrompt = this::class.java.getResource("/server/chat/instructions/1_task.md")!!.readText()
        return GeminiChatService(systemPrompt)
    }
}