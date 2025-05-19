package io.rippledown.kb.chat

import io.rippledown.chat.conversation.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

interface RuleService {
    suspend fun buildRuleToAddComment(case: RDRCase, comment: String)
}

class ChatManager(val conversationService: ConversationService, val ruleService: RuleService) {
    private val logger = lazyLogger
    lateinit var currentCase: RDRCase

    suspend fun startConversation(case: RDRCase): String {
        currentCase = case
        val response = conversationService.startConversation(case)
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        return processActionComment(response.fromJsonString<ActionComment>())
    }

    suspend fun response(message: String): String {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val response = conversationService.response(message)
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE '$response'")
        return processActionComment(response.fromJsonString<ActionComment>())
    }

    //Either pass on the model's response to the user or take some rule action
    suspend fun processActionComment(actionComment: ActionComment): String {
        return when (actionComment.action) {
            STOP_ACTION -> ""  //TODO

            USER_ACTION -> {
                actionComment.message!!
            }

            ADD_ACTION -> {
                val newComment = actionComment.new_comment
                newComment?.let {
                    ruleService.buildRuleToAddComment(currentCase, it)
                    CHAT_BOT_DONE_MESSAGE
                } ?: ""
            }

            REMOVE_ACTION -> "" // TODO
            REPLACE_ACTION -> "" // TODO

            else -> {
                logger.error("Unknown actionComment: ${actionComment.action}")
                ""
            }
        }
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
    }
}

@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val new_comment: String? = null,
    val existing_comment: String? = null
)

