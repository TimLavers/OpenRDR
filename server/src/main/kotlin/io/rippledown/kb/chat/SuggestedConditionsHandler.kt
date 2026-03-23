package io.rippledown.kb.chat

import io.rippledown.chat.FunctionCallHandler
import io.rippledown.model.RDRCase

/**
 * @author Cascade AI
 */
class SuggestedConditionsHandler(
    private val case: RDRCase,
    private val ruleService: RuleService
) : FunctionCallHandler {

    override suspend fun handle(args: Map<String, Any?>): String {
        if (!ruleService.isRuleSessionActive()) {
            return NO_ACTIVE_RULE_SESSION_ERROR
        }
        val addedConditionTexts = ruleService.currentRuleSessionConditionTexts()
        val conditionList = ruleService.conditionHintsForCase(case)
        val suggestions = conditionList.suggestions.filter { it.asText() !in addedConditionTexts }
        if (suggestions.isEmpty()) {
            return "No suggested conditions available for this case."
        }
        return suggestions.mapIndexed { index, suggestion ->
            val editable = if (suggestion.isEditable()) " [editable]" else ""
            "${index + 1}. ${suggestion.asText()}$editable"
        }.joinToString("\n")
    }

    companion object {
        const val NO_ACTIVE_RULE_SESSION_ERROR =
            "Error: No rule session is active. You must first output the AddComment, RemoveComment, or ReplaceComment action to start a rule session before calling getSuggestedConditions."
    }
}
