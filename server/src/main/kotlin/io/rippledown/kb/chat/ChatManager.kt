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
) : ModelResponder {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null

    suspend fun startConversation(viewableCase: ViewableCase): ChatResponse {
        currentCase = viewableCase
        val response = conversationService.startConversation()
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        return processActionComment(response.sanitizeLlmJson().fromJsonString<ActionComment>())
    }

    override suspend fun response(message: String): ChatResponse {
        return processConversationResponse(message)
    }

    private suspend fun processConversationResponse(message: String): ChatResponse {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val response = conversationService.response(message)
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        try {
            // Split response into individual JSON objects and process each
            val jsonFragments = extractJsonFragments(response)
            var lastResponse = ChatResponse("")
            jsonFragments.forEach { fragment ->
                lastResponse = processActionComment(fragment.sanitizeLlmJson().fromJsonString<ActionComment>())
            }
            return lastResponse
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
        return if (!actionComment.suggestions.isNullOrEmpty()) {
            ChatResponse(chatResponse.text, actionComment.suggestions)
        } else {
            chatResponse
        }
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
    }
}

fun String.sanitizeLlmJson() = replace("\\'", "'")
