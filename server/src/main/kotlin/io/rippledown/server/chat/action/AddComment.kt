package io.rippledown.server.chat.action

import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString

data class AddComment(val comment: String) : ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")
        val cornerstoneStatus = ruleService.startRuleSessionToAddComment(sessionCase, comment)
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
    }
}