package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class PreviousCornerstone() : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        val currentStatus = ruleService.cornerstoneStatus()
        val previousIndex = currentStatus.indexOfCornerstoneToReview - 1
        val cornerstoneStatus = ruleService.selectCornerstoneCase(previousIndex)
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }
}
