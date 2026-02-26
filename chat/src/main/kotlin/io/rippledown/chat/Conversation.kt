package io.rippledown.chat

import com.google.genai.Chat
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import io.rippledown.llm.callWithTimeout
import io.rippledown.llm.retry
import io.rippledown.log.lazyLogger
import io.rippledown.stripEnclosingJson
import io.rippledown.toJsonString

interface ConversationService {
    suspend fun startConversation(): String = ""
    suspend fun response(message: String): String = ""
}

interface ReasonTransformer {
    suspend fun transform(reason: String): ReasonTransformation
}

class Conversation(private val chatService: ChatService, private val reasonTransformer: ReasonTransformer) :
    ConversationService {
    private val logger = lazyLogger
    private lateinit var chat: Chat

    override suspend fun startConversation(): String {
        chat = retry {
            chatService.startChat()
        }
        return response("Please assist me with the report for this case.")
    }

    private suspend fun executeFunction(functionCall: FunctionCall): String {
        val name = functionCall.name().orElse("")
        if (name != TRANSFORM_REASON) {
            logger.warn("Unknown function call: $name")
            return "Unknown function: $name"
        }

        val reason = functionCall.args().map { it[REASON_PARAMETER]?.toString() }.orElse("") ?: ""
        val transformation = reasonTransformer.transform(reason)
        return "'$reason' evaluation: ${transformation.toJsonString()}"
    }

    override suspend fun response(message: String): String {
        val currentChat = checkNotNull(chat) { "Chat not initialized. Call startConversation first." }
        val response = try {
            callWithTimeout { currentChat.sendMessage(message) }
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            throw e
        }
        return handleResponse(response)
    }

    private suspend fun handleResponse(response: GenerateContentResponse): String {
        var currentResponse = response
        while (currentResponse.functionCalls()?.isNotEmpty() == true) {
            val functionResults = currentResponse.functionCalls()!!.map { executeFunction(it) }
            val prompt = Content.fromParts(Part.fromText("Function results: ${functionResults.joinToString(", ")}"))
            currentResponse = callWithTimeout { chat.sendMessage(prompt) }
        }
        return currentResponse.text()?.stripEnclosingJson() ?: "No function call or text response"
    }

    /**
     * Logs input and output token counts from a response, or estimates if unavailable.
     */
    private fun logTokenCounts(response: GenerateContentResponse, context: String) {
        logger.info("$context - tokens: ${response.usageMetadata()}")
    }

    companion object {
        const val REASON_PARAMETER = "reason"
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
    }
}