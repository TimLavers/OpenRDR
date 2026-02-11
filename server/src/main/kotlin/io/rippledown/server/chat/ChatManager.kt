package io.rippledown.server.chat

import io.rippledown.chat.ConversationService
import io.rippledown.extractJsonFragments
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.persistence.postgres.KBId
import io.rippledown.server.ServerChatActionsInterface

interface ModelResponder {
    suspend fun response(message: String): String
}

/**
 * Manages the chat conversation with the user, processing messages and actions based on the AI model's responses.
 */
class ChatManager(
    val conversationService: ConversationService,
    private val actionsInterface: ServerChatActionsInterface
) : ModelResponder {
    private val logger = lazyLogger
    private var kbId: String? = null
    private var currentCase: ViewableCase? = null

    suspend fun startConversation(kbId: String?, viewableCase: ViewableCase?): String {
        this.kbId = kbId
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
            // Split the response into individual JSON objects and process each
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
        logger.info("Processing actionComment: $actionComment")
        val action = actionComment.createActionInstance()
        logger.info("action is $action")
        return if (action != null) {
            action.applyAction(actionsInterface, kbId, currentCase, this)
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
