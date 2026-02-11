package io.rippledown.server.chat.action

import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString

class ExemptCornerstone() : ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        val cornerstoneStatus = ruleService.exemptCornerstoneCase()
        ruleService.sendCornerstoneStatus()
        return modelResponder.response(cornerstoneStatus.toJsonString<CornerstoneStatus>())
    }
}