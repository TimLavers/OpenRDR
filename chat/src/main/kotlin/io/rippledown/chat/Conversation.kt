package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.log.lazyLogger
import io.rippledown.stripEnclosingJson
import kotlinx.coroutines.delay
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.milliseconds

interface ConversationService {
    suspend fun startConversation(): String = ""
    suspend fun response(userMessage: String): String = ""
}

interface ExpressionValidator {
    suspend fun isValid(expression: String): Boolean
}

/**
 * Manages a conversation with an AI model, handling user messages, function calls and retries on failure.
 */
class Conversation(private val chatService: ChatService, private val expressionValidator: ExpressionValidator) :
    ConversationService {
    private val logger = lazyLogger
    private lateinit var chat: Chat

    override suspend fun startConversation(): String {
        chat = retry {
            chatService.startChat()
        }
        return response("Please assist me with the report for this case.")
    }

    private suspend fun executeFunction(functionCall: FunctionCallPart): String {
        if (functionCall.name != "isExpressionValid") {
            logger.warn("Unknown function call: ${functionCall.name}")
            return "Unknown function: ${functionCall.name}"
        }

        val expression = functionCall.args?.get("expression") ?: ""
        val isValid = expressionValidator.isValid(expression)
        logger.info("Function call: '${functionCall.name}' with args: '$expression', isValid: $isValid")
        return "'$expression' is valid?: $isValid"
    }

    /**
     * Processes a user message and returns the AI model's response.
     *
     * @param userMessage The user's input message.
     * @return The model's response, possibly after executing function calls.
     */
    override suspend fun response(userMessage: String): String {
        val currentChat = checkNotNull(chat) { "Chat not initialized. Call startConversation first." }
        val response = try {
            currentChat.sendMessage(userMessage)
        } catch (e: Exception) {
            logger.error("Failed to send message: $userMessage", e)
            throw e
        }
        val finalResponse = handleResponse(response)
        logger.info("initial response text: ${response.text}")
        logger.info("final response text  : ${finalResponse}")
        return finalResponse
    }

    private suspend fun handleResponse(response: GenerateContentResponse): String {
        return if (response.functionCalls.isNotEmpty()) {
            val functionResults = response.functionCalls.map { executeFunction(it) }
            val prompt = content { text("Function results: ${functionResults.joinToString(", ")}") }
            val followUpResponse = chat.sendMessage(prompt)
            followUpResponse.text?.stripEnclosingJson() ?: "No text response after function execution"
        } else {
            response.text?.stripEnclosingJson() ?: "No function call or text response"
        }
    }

    /**
     * Logs input and output token counts from a response, or estimates if unavailable.
     */
    private fun logTokenCounts(response: GenerateContentResponse, context: String) {
        logger.info("$context - tokens: ${response.usageMetadata?.totalTokenCount}")
    }

}

/**
 * Retry when receiving the 503 error from the API due to rate limiting.
 */
object Retry

suspend fun <T> retry(
    maxRetries: Int = 10,
    initialDelay: Long = 1_000,
    maxDelay: Long = 32_000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            Retry.lazyLogger.info("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}

