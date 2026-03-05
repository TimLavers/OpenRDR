package io.rippledown.kb.chat

import io.rippledown.chat.FunctionCallHandler
import io.rippledown.model.RDRCase

class SuggestedConditionsHandler(
    private val case: RDRCase,
    private val ruleService: RuleService
) : FunctionCallHandler {
    var latestSuggestions: List<String> = emptyList()
        private set

    override suspend fun handle(args: Map<String, Any?>): String {
        val addedConditionTexts = ruleService.currentRuleSessionConditionTexts()
        val conditionList = ruleService.conditionHintsForCase(case)
        val suggestions = conditionList.suggestions.filter { it.asText() !in addedConditionTexts }
        if (suggestions.isEmpty()) {
            latestSuggestions = emptyList()
            return "No suggested conditions available for this case."
        }
        latestSuggestions = suggestions.map { suggestion ->
            val editable = if (suggestion.isEditable()) " [editable]" else ""
            "${suggestion.asText()}$editable"
        }
        return suggestions.mapIndexed { index, suggestion ->
            val editable = if (suggestion.isEditable()) " [editable]" else ""
            "${index + 1}. ${suggestion.asText()}$editable"
        }.joinToString("\n")
    }

    fun consumeSuggestions(): List<String> {
        val result = latestSuggestions
        latestSuggestions = emptyList()
        return result
    }
}
