package io.rippledown.chat.conversation

import dev.shreyaspatil.ai.client.generativeai.Chat
import io.rippledown.chat.service.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.random.Random.Default.nextLong

interface ConversationService {
    suspend fun startConversation(case: RDRCase): String = ""
    suspend fun response(userMessage: String): String = ""
}

class Conversation : ConversationService {
    val logger = lazyLogger

    private lateinit var chatService: GeminiChatService
    private lateinit var chat: Chat
    private val genericSystemInstruction = this::class.java.getResource("/system-instruction.md")?.readText()
        ?: throw IllegalStateException("Resource file 'system-instruction.md' not found in classpath")

    override suspend fun startConversation(case: RDRCase): String {
        val caseSystemInstruction = insertValuesIntoPromptPlaceholders(case)
        chatService = GeminiChatService(caseSystemInstruction)
        chat = retry {
            chatService.startChat()
        }
        return response("Please assist me with the report for this case.")
    }

    override suspend fun response(userMessage: String): String {
        val response = retry {
            runBlocking {
                chat.sendMessage(userMessage)
            }
        }
        return response.text
            ?.replace("```json\n", "")
            ?.replace("\n```", "")
            ?.trim() ?: ""
    }

    private fun insertValuesIntoPromptPlaceholders(case: RDRCase): String = genericSystemInstruction
        .replace("{{case_json}}", case.toJsonString())
        .replace("{{PLEASE_CONFIRM}}", PLEASE_CONFIRM)
        .replace("{{ADD}}", ADD)
        .replace("{{REMOVE}}", REMOVE)
        .replace("{{REPLACE}}", REPLACE)
        .replace("{{NO_COMMENTS}}", NO_COMMENTS)
        .replace("{{EXISTING_COMMENTS}}", EXISTING_COMMENTS)
        .replace("{{WOULD_YOU_LIKE}}", WOULD_YOU_LIKE)
        .replace("{{ADD_A_COMMENT}}", ADD_A_COMMENT)
        .replace("{{REMOVE_A_COMMENT}}", REMOVE_A_COMMENT)
        .replace("{{REPLACE_A_COMMENT}}", REPLACE_A_COMMENT)
        .replace("{{WHAT_COMMENT}}", WHAT_COMMENT)
        .replace("{{START}}", START_ACTION)
        .replace("{{STOP}}", STOP_ACTION)
        .replace("{{DEBUG}}", DEBUG_ACTION)
        .replace("{{USER}}", USER_ACTION)
        .replace("{{ADD}}", ADD_ACTION)
        .replace("{{REMOVE}}", REMOVE_ACTION)
        .replace("{{REPLACE}}", REPLACE_ACTION)
}

/**
 * Retry when receiving the 503 error from the API due to rate limiting.
 */
fun <T> retry(
    maxRetries: Int = 10,
    initialDelay: Long = 1_000,
    maxDelay: Long = 32_000,
    block: () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            println("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            sleep(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}

