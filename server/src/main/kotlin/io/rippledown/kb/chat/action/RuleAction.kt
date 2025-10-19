package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult

abstract class RuleAction(val comment: String, val reasons: List<String>?): ChatAction {
    abstract suspend fun buildRule(ruleService: RuleService, currentCase: ViewableCase, conditions: List<Condition>)
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?): String {
        val conditionParsingResults = reasons?.map { expression ->
            ruleService.conditionForExpression(currentCase!!.case, expression)
        } ?: emptyList()

        //Check for failures and collect conditions at the same time
        val (failedResult, conditions) = checkForUnparsedConditions(conditionParsingResults)

        //If a failure was found, return the error message
        if (failedResult != null) {
            return "Failed to parse condition: ${failedResult}"
        } else {
            buildRule(ruleService, currentCase!!, conditions)
            return CHAT_BOT_DONE_MESSAGE
        }
    }

    fun checkForUnparsedConditions(conditionParsingResults: List<ConditionParsingResult>): Pair<String?, List<Condition>> {
        val (failedResult, conditions) = conditionParsingResults.fold(
            initial = Pair(first = null as String?, second = mutableListOf<Condition>())
        ) { acc, result ->
            if (acc.first == null && result.isFailure) {
                Pair(result.errorMessage, acc.second)
            } else if (!result.isFailure) {
                val condition = result.condition ?: throw IllegalStateException("Condition should not be null for a successful parsing result")
                acc.second.add(condition)
                acc
            } else {
                acc
            }
        }
        return Pair(failedResult, conditions)
    }
}