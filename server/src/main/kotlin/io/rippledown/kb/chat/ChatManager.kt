package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase

interface ModelResponder {
    suspend fun response(message: String): String
}

/**
 * Manages the chat conversation with the user, processing messages and actions based on the AI model's responses.
 */
class ChatManager(val conversationService: ConversationService, val ruleService: RuleService) : ModelResponder {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null

    suspend fun startConversation(viewableCase: ViewableCase): String {
        currentCase = viewableCase
        val response = conversationService.startConversation()
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        return processActionComment(response.fromJsonString<ActionComment>())
    }

    override suspend fun response(message: String): String {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val response = conversationService.response(message)
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        try {
            // Split response into individual JSON objects and process each
            val jsonFragments = extractJsonFragments(response)
            var lastResponse = ""
            jsonFragments.forEach { fragment ->
                lastResponse = processActionComment(fragment.fromJsonString<ActionComment>())
            }
            return lastResponse
        } catch (e: Exception) {
            logger.error("Failed to process ActionComment: $response", e)
            return "System error. See server.log: '$response'"
        }
    }


    //Either pass on the model's response to the user or take some action
    suspend fun processActionComment(actionComment: ActionComment): String {
        val chatAction = actionComment.createActionInstance()
        return if (chatAction != null) {
            chatAction.doIt(ruleService, currentCase, this)
        } else {
                logger.error("Unknown actionComment: ${actionComment.action}")
                ""
            }
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
    }
}
