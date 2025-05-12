package io.rippledown.chat.conversation

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.Content
import dev.shreyaspatil.ai.client.generativeai.type.TextPart
import io.rippledown.chat.service.GeminiChatService
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface ConversationService {
    suspend fun startConversation(case: RDRCase): String = ""
    suspend fun response(userMessage: String): String = ""
}

const val CONFIRMATION_START = "Please confirm"
const val QUESTION_IF_THERE_ARE_EXISTING_COMMENTS =
    "Would you like to change the report? If so, you can add a comment, modify a comment, or remove a comment."
const val QUESTION_IF_THERE_ARE_NO_EXISTING_COMMENTS = "Would you like to add a comment to this report?"

class Conversation : ConversationService {
    val logger: Logger = LoggerFactory.getLogger("rdr")
//    val logger: Logger = LoggerFactory.getLogger(this::class.java.name)

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
        println("caseSystemInstruction = ${caseSystemInstruction}")
        chatService = GeminiChatService(caseSystemInstruction)
        chat = chatService.startChat()
        logger.info("ChatModel: the following is the initial response:")
        val initialResponse = response("")
        return initialResponse
    }

    override suspend fun response(userMessage: String): String {
        val response = runBlocking {
            chat.sendMessage(userMessage)
        }
        val cleanedResponse = response.text
            ?.replace("```json\n", "")
            ?.replace("\n```", "")
            ?.trim() ?: ""

        logger.info("user message: $userMessage")
        logger.info("response: ${response.text}")
        logger.info("cleaned response: ${cleanedResponse}")
        logger.info("history: ${chat.history.extractTextFromContentList()}")
        return cleanedResponse
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

