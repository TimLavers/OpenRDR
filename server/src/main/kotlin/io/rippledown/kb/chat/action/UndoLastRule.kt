package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase

class UndoLastRule(): ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?): String {
        ruleService.undoLastRule()
        return "rule undone"
    }
}