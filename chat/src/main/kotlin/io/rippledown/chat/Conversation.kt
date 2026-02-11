package io.rippledown.chat

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import dev.shreyaspatil.ai.client.generativeai.type.content
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

class Conversation(private val chatService: ChatService, private val reasonTransformer: ReasonTransformer?) :
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
        if (reasonTransformer == null) return "'$reason' evaluation: No reason transformer available."
        val transformation = reasonTransformer.transform(reason)
        return "'$reason' evaluation: ${transformation.toJsonString()}"
    }

    override suspend fun response(message: String): String {
        val currentChat = checkNotNull(chat) { "Chat not initialized. Call startConversation first." }
        logger.info("about to send message to model: '$message'")
        val response = try {
            currentChat.sendMessage(message)
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            throw e
        }
        val finalResponse = handleResponse(response)
        logger.info("initial response json: ${response.text}")
        logger.info("final response json  : ${finalResponse}")
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

    companion object {
        const val REASON_PARAMETER = "reason"
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
    }
}