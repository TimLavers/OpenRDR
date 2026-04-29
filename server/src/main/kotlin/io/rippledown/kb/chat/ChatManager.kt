package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

interface ModelResponder {
    suspend fun response(message: String): ChatResponse
}

/**
 * Manages the chat conversation with the user, processing messages and actions based on the AI model's responses.
 *
 * @author Cascade AI
 */
class ChatManager(
    val conversationService: ConversationService,
    val ruleService: RuleService,
    private val suggestionsBuffer: SuggestionsBuffer = SuggestionsBuffer(),
) : ModelResponder {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null

    suspend fun startConversation(viewableCase: ViewableCase): ChatResponse {
        currentCase = viewableCase
        val response = try {
            conversationService.startConversation()
        } catch (e: Exception) {
            logger.error("Failed to start conversation", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        // When the case already has comments the model replies in prose
        // (e.g. "This case has the following comments: ... Would you
        // like to add another one, or replace or remove one of them?")
        // instead of emitting a JSON ActionComment. Mirror the robustness
        // of `processConversationResponse` here: extract any JSON
        // fragments and, if there are none, surface the raw text as a
        // plain bot message rather than 500ing.
        return try {
            val jsonFragments = extractJsonFragments(response)
            if (jsonFragments.isEmpty()) {
                ChatResponse(response)
            } else {
                processActionComment(jsonFragments.first().sanitizeLlmJson().fromJsonString<ActionComment>())
            }
        } catch (e: Exception) {
            logger.error("Failed to process start-conversation ActionComment: $response", e)
            ChatResponse(response)
        }
    }

    override suspend fun response(message: String): ChatResponse {
        return processConversationResponse(message)
    }

    private suspend fun processConversationResponse(message: String): ChatResponse {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val messageToSend = augmentWithCornerstoneStatus(message)
        val response = try {
            conversationService.response(messageToSend)
        } catch (e: Exception) {
            logger.error("Failed to send message: $message", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        try {
            // Extract the first JSON object from the response (the model may sometimes
            // return multiple JSON objects, but only the first should be processed since
            // actions like ExemptCornerstone handle continuations via recursive calls)
            val jsonFragments = extractJsonFragments(response)
            if (jsonFragments.isEmpty()) {
                return ChatResponse("")
            }
            return processActionComment(jsonFragments.first().sanitizeLlmJson().fromJsonString<ActionComment>())
        } catch (e: Exception) {
            logger.error("Failed to process ActionComment: $response", e)
            return ChatResponse("System error. See server.log: '$response'")
        }
    }

    //Either pass on the model's response to the user or take some action
    suspend fun processActionComment(actionComment: ActionComment): ChatResponse {
        val chatAction = actionComment.createActionInstance()
        val chatResponse = if (chatAction != null) {
            chatAction.doIt(ruleService, currentCase, this)
        } else {
            logger.error("Unknown actionComment: ${actionComment.action}")
            ChatResponse("")
        }
        val bufferedSuggestions = suggestionsBuffer.consume()
        return when {
            bufferedSuggestions != null -> ChatResponse(chatResponse.text, bufferedSuggestions)
            !actionComment.suggestions.isNullOrEmpty() -> ChatResponse(chatResponse.text, actionComment.suggestions)
            else -> chatResponse
        }
    }

    private fun augmentWithCornerstoneStatus(message: String): String {
        if (!ruleService.isRuleSessionActive()) return message
        val status = ruleService.cornerstoneStatus()
        return "$CURRENT_CORNERSTONE_STATUS_PREFIX${status.summary()}]\n$message"
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
        const val AI_UNAVAILABLE_MESSAGE = "The AI assistant is temporarily unavailable. Please try again later."
        const val CURRENT_CORNERSTONE_STATUS_PREFIX = "[Current cornerstone status: "
    }
}

fun String.sanitizeLlmJson() = replace("\\'", "'").replace("\\\\n", "\\n")
