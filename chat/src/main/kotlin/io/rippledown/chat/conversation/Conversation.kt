package io.rippledown.chat.conversation

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.TextPart
import io.rippledown.chat.service.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface ConversationService {
    suspend fun startConversation(case: RDRCase): String = ""
    suspend fun response(userMessage: String): String = ""
}

class Conversation : ConversationService {
    val logger: Logger = LoggerFactory.getLogger(this::class.java.name)

    private lateinit var chatService: GeminiChatService
    private lateinit var chat: Chat
    private val genericSystemInstruction = this::class.java.getResource("/system-instruction.md")?.readText()
        ?: throw IllegalStateException("Resource file 'system-instruction.md' not found in classpath")

    override suspend fun startConversation(case: RDRCase): String {
        val caseSystemInstruction = genericSystemInstruction
            .replace("{{case_json}}", case.toJsonString())
            .replace("{{confirmation_start}}", CONFIRMATION_START)
            .replace("{{question_if_there_are_existing_comments}}", QUESTION_IF_THERE_ARE_EXISTING_COMMENTS)
            .replace("{{question_if_there_are_no_existing_comments}}", QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS)
            .replace("{{DEBUG}}", DEBUG_ACTION)
            .replace("{{USER}}", USER_ACTION)
            .replace("{{ADD}}", ADD_ACTION)
            .replace("{{REMOVE}}", REMOVE_ACTION)
            .replace("{{REPLACE}}", REPLACE_ACTION)
        println("caseSystemInstruction = ${caseSystemInstruction}")
        chatService = GeminiChatService(caseSystemInstruction)
        chat = chatService.startChat()
        return response("")
    }

    override suspend fun response(userMessage: String): String {
        val response = runBlocking {
            chat.sendMessage(userMessage)
        }
        return response.text
            ?.replace("```json\n", "")
            ?.replace("\n```", "")
            ?.trim() ?: ""
    }
}

fun List<Content>.extractTextFromContentList(): String {
    return joinToString(separator = "\n") { content ->
        content.parts.filterIsInstance<TextPart>()
            .joinToString { part ->
                part.text
            }
    }
}

