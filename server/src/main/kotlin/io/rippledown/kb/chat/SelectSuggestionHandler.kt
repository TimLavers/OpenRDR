package io.rippledown.kb.chat

import io.rippledown.chat.Conversation.Companion.CONDITION_TEXT_PARAMETER
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.chat.ReasonTransformation
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString

class SelectSuggestionHandler(
    private val case: RDRCase,
    private val ruleService: RuleService,
) : FunctionCallHandler {
    override suspend fun handle(args: Map<String, Any?>): String {
        val conditionText = args[CONDITION_TEXT_PARAMETER]?.toString() ?: ""
        val condition = ruleService.conditionForSuggestionText(case, conditionText)
            ?: return "'$conditionText' evaluation: ${ReasonTransformation(message = "Could not find a matching non-editable suggestion for '$conditionText'. Please use the $TRANSFORM_REASON function instead.").toJsonString()}"
        ruleService.addConditionToCurrentRuleSession(condition)
        val cornerstoneStatus = ruleService.cornerstoneStatus()
        ruleService.sendCornerstoneStatus()
        val transformation = ReasonTransformation(condition.id(), "Ok", cornerstoneStatus.toJsonString())
        val result = "'$conditionText' evaluation: ${transformation.toJsonString()}"
        return "$result\nCornerstone status: ${cornerstoneStatus.toJsonString()}"
    }

    companion object {
        const val TRANSFORM_REASON = "transformReasonToFormalCondition"
    }
}
