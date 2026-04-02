package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class NextCornerstone() : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        val currentStatus = ruleService.cornerstoneStatus()
        val nextIndex = currentStatus.indexOfCornerstoneToReview + 1
        val cornerstoneStatus = ruleService.selectCornerstoneCase(nextIndex)
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.summary())
    }
}
