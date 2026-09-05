package io.rippledown.chat

import com.google.genai.Chat
import com.google.genai.types.FunctionCall
import com.google.genai.types.GenerateContentResponse
import io.rippledown.llm.callWithTimeout
import io.rippledown.llm.geminiApiCallCount
import io.rippledown.llm.retry
import io.rippledown.log.lazyLogger
import io.rippledown.stripEnclosingJson
import java.util.concurrent.TimeoutException

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
    private val functionCallHandlers: Map<String, FunctionCallHandler>,
    private val openingMessage: String? = DEFAULT_OPENING_MESSAGE
) :
    ConversationService {
    private val logger = lazyLogger
    private lateinit var chat: Chat

    override suspend fun startConversation(): String {
        chat = retry {
            chatService.startChat()
        }
        return openingMessage?.let { response(it) } ?: ""
    }

    private suspend fun executeFunction(functionCall: FunctionCall): String {
        val name = functionCall.name().orElse("")
        val handler = functionCallHandlers[name]
        if (handler == null) {
            logger.warn("Unknown function call: $name")
            return "Error: '$name' is not a callable function. If '$name' is an action, do NOT call it " +
                    "via the function-calling API. Instead, your VERY NEXT response MUST be a single JSON " +
                    "object with \"action\": \"$name\" (plus any fields that action requires). Output ONLY " +
                    "that JSON object now — do NOT apologise, do NOT write prose, and do NOT tell the user the " +
                    "action is unavailable."
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
    private fun sendMessageHandlingEmptyContent(message: String): GenerateContentResponse {
        var lastException: Exception? = null
        repeat(MAX_TIMEOUT_RETRIES + 1) { attempt ->
            try {
                return callWithTimeout(SEND_TIMEOUT_MS) { chat.sendMessage(message) }
            } catch (e: NoSuchElementException) {
                logger.warn("Gemini returned a candidate with no content; retrying with a nudge", e)
                return callWithTimeout(SEND_TIMEOUT_MS) { chat.sendMessage("$message\n\n$CONTINUE_NUDGE") }
            } catch (e: RuntimeException) {
                if (e.cause !is TimeoutException) throw e
                lastException = e
                if (attempt < MAX_TIMEOUT_RETRIES) {
                    val delayMs = TIMEOUT_RETRY_DELAY_MS * (attempt + 1)
                    logger.warn("Gemini API call timed out (attempt ${attempt + 1} of $MAX_TIMEOUT_RETRIES); retrying in ${delayMs}ms")
                    Thread.sleep(delayMs)
                }
            }
        }
        throw lastException ?: RuntimeException("Unexpected: exhausted retries without exception")
    }

    internal suspend fun handleResponse(response: GenerateContentResponse, emptyResponseRetries: Int = 0): String {
        logTokenCounts(response, "Turn")
        var currentResponse = usableOrNull(response)
        while (true) {
            val functionCalls = currentResponse?.functionCalls() ?: emptyList()
            if (functionCalls.isEmpty()) break
            val functionResults = functionCalls.map { executeFunction(it) }
            val next = sendMessageHandlingEmptyContent("Function results: ${functionResults.joinToString(", ")}")
            logTokenCounts(next, "Turn after function results")
            currentResponse = usableOrNull(next)
        }
        val text = currentResponse?.text()?.stripEnclosingJson()
        if (text != null) {
            return text
        }
        logEmptyResponse(currentResponse ?: response)
        if (emptyResponseRetries < MAX_EMPTY_RESPONSE_RETRIES) {
            logger.info("Retrying after empty response (attempt ${emptyResponseRetries + 1} of $MAX_EMPTY_RESPONSE_RETRIES)...")
            return handleResponse(sendMessageHandlingEmptyContent(CONTINUE_NUDGE), emptyResponseRetries + 1)
        }
        return "No function call or text response"
    }

    /**
     * Return [response] if its content can be read, or null if the turn finished for a reason that makes the
     * content inaccessible.
     *
     * When Gemini ends a turn with a finish reason such as `MALFORMED_FUNCTION_CALL`, google-genai's
     * `GenerateContentResponse.checkFinishReason` throws [IllegalArgumentException] from both `functionCalls()`
     * and `text()`, so there is nothing to hand back to the caller. The SDK does not add such a response to the
     * curated history, so the chat is still usable: returning null lets the caller fall through to the
     * empty-response retry, which nudges the model into producing a well-formed turn instead of failing the
     * whole user message (which would leave any rule session in progress stranded).
     */
    private fun usableOrNull(response: GenerateContentResponse): GenerateContentResponse? =
        try {
            response.functionCalls()
            response
        } catch (e: IllegalArgumentException) {
            logger.warn("Gemini response content is inaccessible; treating it as an empty response", e)
            null
        }

    private fun logEmptyResponse(response: GenerateContentResponse) {
        logger.warn("Model returned no text and no function calls. Response details: candidates=${response.candidates()}, usageMetadata=${response.usageMetadata()}")
    }

    /**
     * Log the token counts for a completed turn.
     *
     * `prompt` grows with the conversation, so it shows how much history each
     * turn is carrying; `candidates` is what the model actually generated, so a
     * sudden jump there is the signature of a runaway generation. Logged for
     * every turn so the trend up to a slow or hung call can be read off the log.
     *
     * Note this can only ever report on calls that returned - a call that hangs
     * until it is abandoned produces no usage metadata at all.
     */
    private fun logTokenCounts(response: GenerateContentResponse, context: String) {
        val usage = try {
            response.usageMetadata().orElse(null)
        } catch (e: Exception) {
            logger.info("$context - tokens: unavailable (${e.message})")
            return
        }
        if (usage == null) {
            logger.info("$context - tokens: unavailable")
            return
        }
        val prompt = usage.promptTokenCount().orElse(null)
        val candidates = usage.candidatesTokenCount().orElse(null)
        val total = usage.totalTokenCount().orElse(null)
        val n = geminiApiCallCount.get()
        logger.info("$context (API call #$n) - tokens: prompt=$prompt, candidates=$candidates, total=$total")
    }

    companion object {
        /**
         * Per-turn timeout for a chat send.
         *
         * A user is waiting on every one of these, so the cap is well below
         * [callWithTimeout]'s batch-oriented default, for the same reason that
         * `ReportService` caps its own interactive call. Turns normally complete
         * in one to three seconds, so this is ample headroom: a call still
         * running after 30s has hung rather than merely slowed, and waiting
         * longer only delays telling the user so.
         *
         * Keeping it comfortably under any client-side wait also matters for
         * diagnosis. With one retry the server's worst case is 30s + 5s + 30s
         * = 65s; the client's chat-message timeout (90s) outlasts that so the
         * server's own "AI unavailable" response reaches the user instead of
         * the client masking it with a generic failure.
         */
        const val SEND_TIMEOUT_MS = 30_000L

        const val MAX_EMPTY_RESPONSE_RETRIES = 2
        const val MAX_TIMEOUT_RETRIES = 1
        const val TIMEOUT_RETRY_DELAY_MS = 5_000L
        const val CONTINUE_NUDGE = "Please continue with the appropriate response."
        const val DEFAULT_OPENING_MESSAGE = "Please assist me with the report for this case."
        const val REASON_PARAMETER = "reason"
        const val CONDITION_TEXT_PARAMETER = "conditionText"
        const val SUGGESTION_NUMBER_PARAMETER = "suggestionNumber"
        const val NEW_VALUE_PARAMETER = "newValue"
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
        const val GET_SUGGESTED_CONDITIONS = "getSuggestedConditions"
        const val SELECT_SUGGESTED_CONDITION = "selectSuggestedCondition"
    }
}
