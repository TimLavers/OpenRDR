package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import dev.shreyaspatil.ai.client.generativeai.type.TextPart
import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.log.lazyLogger
import io.rippledown.stripEnclosingJson
import io.rippledown.toJsonString
import kotlinx.coroutines.delay
import kotlin.random.Random.Default.nextLong
import kotlin.time.Duration.Companion.milliseconds

interface ConversationService {
    suspend fun startConversation(): String = ""
    suspend fun response(userMessage: String): String = ""
}

interface REASON_TRANSFORMER {
    suspend fun transform(reason: String): ReasonTransformation
}

class Conversation(private val chatService: ChatService, private val reasonTransformer: REASON_TRANSFORMER) :
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
        if (functionCall.name != TRANSFORM_REASON) {
            logger.warn("Unknown function call: ${functionCall.name}")
            return "Unknown function: ${functionCall.name}"
        }

        val reason = functionCall.args?.get(REASON_PARAMETER) ?: ""
        val transformation = reasonTransformer.transform(reason)
        return "'$reason' evaluation: ${transformation.toJsonString()}"
    }

    override suspend fun response(userMessage: String): String {
        val currentChat = checkNotNull(chat) { "Chat not initialized. Call startConversation first." }
        val response = try {
            currentChat.sendMessage(userMessage)
        } catch (e: Exception) {
            logger.error("Failed to send message: $userMessage", e)
            throw e
        }
        val finalResponse = handleResponse(response)
        logger.info("initial response json: ${response.text}")
        logger.info("final response json  : ${finalResponse}")
        return finalResponse
    }

    private suspend fun handleResponse(response: GenerateContentResponse): String {
        return if (response.functionCalls.isNotEmpty()) {
            logger.info("*****Received function calls: ${response.functionCalls.joinToString { it.name }}")
            val functionResults = response.functionCalls.map { executeFunction(it) }
            val prompt = content { text("Function results: ${functionResults.joinToString(", ")}") }
            val promptText = (prompt.parts.get(0) as TextPart).text
            logger.info("prompt for follow-up response: '$promptText'")
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

    companion object {
        const val REASON_PARAMETER = "reason"
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
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

