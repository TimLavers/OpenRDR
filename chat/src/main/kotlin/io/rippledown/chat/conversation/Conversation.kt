package io.rippledown.chat.conversation

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import io.rippledown.chat.service.GeminiChatService
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking

interface ConversationService {
    suspend fun startConversation(case: RDRCase): String = ""
    suspend fun response(userMessage: String, case: RDRCase): String = ""
}

const val CONFIRMATION_START = "Please confirm"

class Conversation : ConversationService {
    private lateinit var chatService: GeminiChatService
    private lateinit var chat: Chat
    private lateinit var history: List<Content>
    private val genericSystemInstruction = this::class.java.getResource("/system-instruction.md")?.readText()
        ?: throw IllegalStateException("Resource file 'system-instruction.md' not found in classpath")

    override suspend fun startConversation(case: RDRCase): String {
        val caseSystemInstruction = genericSystemInstruction
            .replace("{{case_json}}", case.toJsonString())
            .replace("{{confirmation_start}}", CONFIRMATION_START)
        chatService = GeminiChatService(caseSystemInstruction)
        history = mutableListOf()
        chat = chatService.startChat(history = history)
        return sendUserMessage("")
    }

    fun sendUserMessage(userMessage: String): String {
        val response = runBlocking {
            chat.sendMessage(userMessage)
        }

        println("ChatModel: user message: $userMessage")
        println("ChatModel: response:     ${response.text}")
        return response.text ?: ""
    }

}