package io.rippledown.kb.chat

import io.rippledown.chat.conversation.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.llm.logger
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

interface RuleService {
    suspend fun buildRuleToAddComment(case: RDRCase, comment: String)
}

class ChatManager(val conversationService: ConversationService, val ruleService: RuleService) {
    lateinit var currentCase: RDRCase

    suspend fun startConversation(case: RDRCase): String {
        currentCase = case
        val response = conversationService.startConversation(case)
        logger.info("Start conversation response: '${response}'")
        return parseAndActionTheModelResponse(response)
    }

    suspend fun response(message: String): String {
        val response = conversationService.response(message)
        logger.info("Response: '${response}'")
        return parseAndActionTheModelResponse(response)
    }

    private suspend fun parseAndActionTheModelResponse(response: String): String {
        val actionComment = response.fromJsonString<ActionComment>()
        return when (actionComment.action) {
            DEBUG_ACTION -> {
                logger.info(actionComment.message)
                ""
            }

            USER_ACTION -> {
                actionComment.message ?: ""
            }

            ADD_ACTION -> {
                val newComment = actionComment.new_comment
                newComment?.let {
                    ruleService.buildRuleToAddComment(currentCase, it)
                    CHAT_BOT_ADD_COMMENT_USER_MESSAGE.replace(ADD_COMMENT_PLACEHOLDER, newComment)
                } ?: ""
            }

            else -> {
                logger.error("Unknown action in response: ${response}")
                ""
            }
        }
    }
}

@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val new_comment: String? = null,
    val existing_comment: String? = null
)

