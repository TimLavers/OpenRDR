package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString

interface RuleService {
    /**
     * Creates a session if not already started, then builds a rule to add a comment
     */
    suspend fun buildRuleToAddComment(viewableCase: ViewableCase, comment: String, conditions: List<Condition>)

    /**
     * Creates a session if not already started, then builds a rule to remove a comment
     */
    suspend fun buildRuleToRemoveComment(viewableCase: ViewableCase, comment: String, conditions: List<Condition>)

    /**
     * Creates a session if not already started, then builds a rule to replace a comment
     */
    suspend fun buildRuleToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        conditions: List<Condition>
    )
    suspend fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult
    fun startCornerstoneReviewSessionToAddComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startCornerstoneReviewSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startCornerstoneReviewSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus

    fun undoLastRule()

    fun moveAttributeTo(moved: String, destination: String)
}

/**
 * Manages the chat conversation with the user, processing messages and actions based on the AI model's responses.
 */
class ChatManager(val conversationService: ConversationService, val ruleService: RuleService) {
    private val logger = lazyLogger
    private var currentCase: ViewableCase? = null

    suspend fun startConversation(viewableCase: ViewableCase): String {
        currentCase = viewableCase
        val response = conversationService.startConversation()
        logger.info("$LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE '$response'")
        return processActionComment(response.fromJsonString<ActionComment>())
    }

    suspend fun response(message: String): String {
        logger.info("$LOG_PREFIX_FOR_USER_MESSAGE '$message'")
        val response = conversationService.response(message)
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        try {
            return processActionComment(response.fromJsonString<ActionComment>())
        } catch (_: Exception) {
            logger.error("Failed to parse response to ActionComment: $response")
            return "response parsing error: '$response'"
        }
    }

    //Either pass on the model's response to the user or take some action
    suspend fun processActionComment(actionComment: ActionComment): String {
        logger.info("---Processing action comment: ${actionComment.toJsonString()}")
        val chatAction = actionComment.createActionInstance()
        if (chatAction != null) {
            return chatAction.doIt(ruleService,currentCase)
        }
        return when (actionComment.action) {
            USER_ACTION -> {
                actionComment.message ?: ""
            }
            REVIEW_CORNERSTONES_ADD_COMMENT -> {
                val comment = actionComment.comment!!
                val sessionCase = currentCase ?: throw IllegalStateException("No current case")
                val cornerstoneStatus = ruleService.startCornerstoneReviewSessionToAddComment(sessionCase, comment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

            REVIEW_CORNERSTONES_REMOVE_COMMENT -> {
                val comment = actionComment.comment!!
                val sessionCase = currentCase ?: throw IllegalStateException("No current case")
                val cornerstoneStatus = ruleService.startCornerstoneReviewSessionToRemoveComment(sessionCase, comment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

            REVIEW_CORNERSTONES_REPLACE_COMMENT -> {
                val comment = actionComment.comment!!
                val replacementComment = actionComment.replacementComment!!
                val sessionCase = currentCase ?: throw IllegalStateException("No current case")
                val cornerstoneStatus =
                    ruleService.startCornerstoneReviewSessionToReplaceComment(sessionCase, comment, replacementComment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

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