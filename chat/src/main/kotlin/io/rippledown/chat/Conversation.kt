package io.rippledown.chat

import com.google.genai.Chat
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.rippledown.llm.callWithTimeout
import io.rippledown.llm.retry
import io.rippledown.log.lazyLogger
import io.rippledown.stripEnclosingJson

interface ConversationService {
    suspend fun startConversation(): String = ""
    suspend fun response(message: String): String = ""
}

interface ReasonTransformer {
    suspend fun transform(reason: String): ReasonTransformation
}

interface FunctionCallHandler {
    suspend fun handle(args: Map<String, Any?>): String
}

class Conversation(
    private val chatService: ChatService,
    private val functionCallHandlers: Map<String, FunctionCallHandler>
) :
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
        val handler = functionCallHandlers[name]
        if (handler == null) {
            logger.warn("Unknown function call: $name")
            return "Unknown function: $name"
        }
        val args = functionCall.args().orElse(emptyMap()).mapValues { it.value }
        return handler.handle(args)
    }

    override suspend fun response(message: String): String {
        val currentChat = checkNotNull(chat) { "Chat not initialized. Call startConversation first." }
        val response = try {
            sendMessageHandlingEmptyContent(message)
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            throw e
        }
        return handleResponse(response)
    }

    /**
     * Send [message] on the multi-turn chat, recovering from a google-genai SDK defect.
     *
     * google-genai 1.46.0's `ChatBase.updateHistoryNonStreaming` calls `Optional.get()` on the first
     * candidate's content and throws [NoSuchElementException] when the model returns a candidate with
     * no content (an empty turn). The SDK throws before recording the turn in history, so the chat is
     * left in a consistent state and the send can be retried. Because the model runs at temperature 0,
     * re-sending the identical input would deterministically reproduce the empty turn, so the retry
     * appends an explicit nudge to vary the input and elicit a non-empty response.
     */
    private fun sendMessageHandlingEmptyContent(message: String): GenerateContentResponse =
        try {
            callWithTimeout { chat.sendMessage(message) }
        } catch (e: NoSuchElementException) {
            logger.warn("Gemini returned a candidate with no content; retrying with a nudge", e)
            callWithTimeout { chat.sendMessage("$message\n\n$CONTINUE_NUDGE") }
        }

    internal suspend fun handleResponse(response: GenerateContentResponse, emptyResponseRetries: Int = 0): String {
        var currentResponse = response
        while (currentResponse.functionCalls()?.isNotEmpty() == true) {
            val functionResults = currentResponse.functionCalls()!!.map { executeFunction(it) }
            currentResponse = sendMessageHandlingEmptyContent("Function results: ${functionResults.joinToString(", ")}")
        }
        val text = currentResponse.text()?.stripEnclosingJson()
        if (text != null) {
            return text
        }
        logEmptyResponse(currentResponse)
        if (emptyResponseRetries < MAX_EMPTY_RESPONSE_RETRIES) {
            logger.info("Retrying after empty response (attempt ${emptyResponseRetries + 1} of $MAX_EMPTY_RESPONSE_RETRIES)...")
            currentResponse = sendMessageHandlingEmptyContent(CONTINUE_NUDGE)
            return handleResponse(currentResponse, emptyResponseRetries + 1)
        }
        return "No function call or text response"
    }

    private fun logEmptyResponse(response: GenerateContentResponse) {
        logger.warn("Model returned no text and no function calls. Response details: candidates=${response.candidates()}, usageMetadata=${response.usageMetadata()}")
    }

    /**
     * Logs input and output token counts from a response, or estimates if unavailable.
     */
    private fun logTokenCounts(response: GenerateContentResponse, context: String) {
        logger.info("$context - tokens: ${response.usageMetadata()}")
    }

    companion object {
        const val MAX_EMPTY_RESPONSE_RETRIES = 2
        const val CONTINUE_NUDGE = "Please continue with the appropriate response."
        const val REASON_PARAMETER = "reason"
        const val CONDITION_TEXT_PARAMETER = "conditionText"
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
        const val GET_SUGGESTED_CONDITIONS = "getSuggestedConditions"
        const val SELECT_SUGGESTED_CONDITION = "selectSuggestedCondition"
    }
}
