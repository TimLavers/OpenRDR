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
    private lateinit var currentCase: ViewableCase

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
            return chatAction.doIt(ruleService)
        }
        return when (actionComment.action) {
            USER_ACTION -> {
                actionComment.message ?: ""
            }
            ADD_COMMENT -> {
                val comment = actionComment.comment!! //TODO remove !!
                val userExpressionsForConditions = actionComment.reasons
                val conditionParsingResults = userExpressionsForConditions?.map { expression ->
                    ruleService.conditionForExpression(currentCase.case, expression)
                } ?: emptyList()

                //Check for failures and collect conditions at the same time
                val (failedResult, conditions) = checkForUnparsedConditions(conditionParsingResults)

                //If a failure was found, return the error message
                if (failedResult != null) {
                    "Failed to parse condition: ${failedResult}"
                } else {
                    ruleService.buildRuleToAddComment(currentCase, comment, conditions)
                    CHAT_BOT_DONE_MESSAGE
                }
            }

            REMOVE_COMMENT -> {
                val comment = actionComment.comment!!
                val userExpressionsForConditions = actionComment.reasons
                val conditionParsingResults = userExpressionsForConditions?.map { expression ->
                    ruleService.conditionForExpression(currentCase.case, expression)
                } ?: emptyList()
                conditionParsingResults.forEach { condition -> logger.info("error parsing condition ${condition.errorMessage}") }

                //Check for failures and collect conditions at the same time
                val (failedResult, conditions) = checkForUnparsedConditions(conditionParsingResults)

                // If a failure was found, return the error message
                if (failedResult != null) {
                    "Failed to parse condition: ${failedResult}"
                } else {
                    ruleService.buildRuleToRemoveComment(currentCase, comment, conditions)
                    CHAT_BOT_DONE_MESSAGE
                }
            }

            REPLACE_COMMENT -> {
                val comment = actionComment.comment!!
                val replacementComment = actionComment.replacementComment!!
                val userExpressionsForConditions = actionComment.reasons
                val conditionParsingResults = userExpressionsForConditions?.map { expression ->
                    ruleService.conditionForExpression(currentCase.case, expression)
                } ?: emptyList()
                conditionParsingResults.forEach { condition -> logger.info("error parsing condition ${condition.errorMessage}") }

                //Check for failures and collect conditions at the same time
                val (failedResult, conditions) = checkForUnparsedConditions(conditionParsingResults)

                // If a failure was found, return the error message
                if (failedResult != null) {
                    "Failed to parse condition: ${failedResult}"
                } else {
                    ruleService.buildRuleToReplaceComment(currentCase, comment, replacementComment, conditions)
                    CHAT_BOT_DONE_MESSAGE
                }
            }

            REVIEW_CORNERSTONES_ADD_COMMENT -> {
                val comment = actionComment.comment!!
                val cornerstoneStatus = ruleService.startCornerstoneReviewSessionToAddComment(currentCase, comment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

            REVIEW_CORNERSTONES_REMOVE_COMMENT -> {
                val comment = actionComment.comment!!
                val cornerstoneStatus = ruleService.startCornerstoneReviewSessionToRemoveComment(currentCase, comment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

            REVIEW_CORNERSTONES_REPLACE_COMMENT -> {
                val comment = actionComment.comment!!
                val replacementComment = actionComment.replacementComment!!
                val cornerstoneStatus =
                    ruleService.startCornerstoneReviewSessionToReplaceComment(currentCase, comment, replacementComment)
                response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
            }

            else -> {
                logger.error("Unknown actionComment: ${actionComment.action}")
                ""
            }
        }
    }

    // Checks for unparsed conditions in the list of ConditionParsingResults.
    // Returns a pair containing an error message (if any) and a list of successfully parsed conditions.
    fun checkForUnparsedConditions(conditionParsingResults: List<ConditionParsingResult>): Pair<String?, List<Condition>> {
        val (failedResult, conditions) = conditionParsingResults.fold(
            initial = Pair(first = null as String?, second = mutableListOf<Condition>())
        ) { acc, result ->
            if (acc.first == null && result.isFailure) {
                Pair(result.errorMessage, acc.second)
            } else if (!result.isFailure) {
                acc.second.add(
                    result.condition
                        ?: throw IllegalStateException("Condition should not be null for a successful parsing result")
                )
                acc
            } else {
                acc
            }
        }
        return Pair(failedResult, conditions)
    }

    companion object {
        const val LOG_PREFIX_FOR_START_CONVERSATION_RESPONSE = "Start conversation response:"
        const val LOG_PREFIX_FOR_CONVERSATION_RESPONSE = "Conversation response:"
        const val LOG_PREFIX_FOR_USER_MESSAGE = "User message:"
    }
}

