package io.rippledown.kb.chat

import io.rippledown.chat.conversation.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.fromJsonString
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult

interface RuleService {
    suspend fun buildRuleToAddComment(case: RDRCase, comment: String, conditions: List<Condition>)
    suspend fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult
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
        logger.info("$LOG_PREFIX_FOR_CONVERSATION_RESPONSE $response")
        return processActionComment(response.fromJsonString<ActionComment>())
    }

    //Either pass on the model's response to the user or take some rule action
    suspend fun processActionComment(actionComment: ActionComment): String {
        return when (actionComment.action) {

            USER_ACTION -> {
                actionComment.message ?: ""
            }

            ADD_ACTION -> {
                val newComment = actionComment.new_comment
                val userExpressionsForConditions = actionComment.conditions
                val conditionParsingResults = userExpressionsForConditions?.map { expression ->
                    ruleService.conditionForExpression(currentCase, expression)
                } ?: emptyList()
                conditionParsingResults.forEach { condition -> logger.info("error parsing condition ${condition.errorMessage}") }

                //Check for failures and collect conditions at the same time
                val (failedResult, conditions) = checkForUnparsedConditions(conditionParsingResults)

                // If a failure was found, return the error message
                if (failedResult != null) {
                    "Failed to parse condition: ${failedResult}"
                } else {
                    newComment?.let {
                        ruleService.buildRuleToAddComment(currentCase, it, conditions)
                        CHAT_BOT_DONE_MESSAGE
                    } ?: ""
                }
            }

            STOP_ACTION -> ""  //TODO
            REMOVE_ACTION -> "" // TODO
            REPLACE_ACTION -> "" // TODO

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

