package io.rippledown.server.chat.action

import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase

class UndoLastRule(): ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        ruleService.undoLastRuleSession()
        return "rule undone"
    }
}