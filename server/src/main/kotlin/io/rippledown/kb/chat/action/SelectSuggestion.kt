package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

data class SelectSuggestion(val comment: String) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")
        val condition = ruleService.conditionForSuggestionText(sessionCase.case, comment)
            ?: return modelResponder.response(
                "Could not find a matching non-editable suggestion for '$comment'. " +
                        "Please use the transformReasonToFormalCondition function instead."
            )
        ruleService.addConditionToCurrentRuleSession(condition)
        val cornerstoneStatus = ruleService.cornerstoneStatus()
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }
}
