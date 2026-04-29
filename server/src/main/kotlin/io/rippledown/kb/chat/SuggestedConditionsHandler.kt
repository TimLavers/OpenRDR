package io.rippledown.kb.chat

import io.rippledown.chat.FunctionCallHandler
import io.rippledown.model.RDRCase

/**
 * @author Cascade AI
 */
class SuggestedConditionsHandler(
    private val case: RDRCase,
    private val ruleService: RuleService,
    private val suggestionsBuffer: SuggestionsBuffer = SuggestionsBuffer()
) : FunctionCallHandler {

    override suspend fun handle(args: Map<String, Any?>): String {
        if (!ruleService.isRuleSessionActive()) {
            return NO_ACTIVE_RULE_SESSION_ERROR
        }
        val addedConditionTexts = ruleService.currentRuleSessionConditionTexts()
        val conditionList = ruleService.conditionHintsForCase(case)
        val suggestions = conditionList.suggestions.filter { it.asText() !in addedConditionTexts }
        if (suggestions.isEmpty()) {
            suggestionsBuffer.suggestions = emptyList()
            return "No suggested conditions available for this case."
        }
        val suggestionTexts = suggestions.map { suggestion ->
            val editable = if (suggestion.isEditable()) " $EDITABLE_MARKER" else ""
            "${suggestion.asText()}$editable"
        }
        suggestionsBuffer.suggestions = suggestionTexts
        return SUGGESTIONS_DELIVERED_INSTRUCTION
    }

    companion object {
        const val NO_ACTIVE_RULE_SESSION_ERROR =
            "Error: No rule session is active. You must first output the AddComment, RemoveComment, or ReplaceComment action to start a rule session before calling getSuggestedConditions."
        const val EDITABLE_MARKER = "[editable]"
        const val SUGGESTIONS_DELIVERED_INSTRUCTION =
            "Suggested conditions have already been displayed to the user by the system. " +
                    "Reply with a brief UserAction message asking the user to select a condition or enter their own reason. " +
                    "Do NOT include a 'suggestions' array in the JSON, and do NOT list the suggestions in the message."
    }
}
