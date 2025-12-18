package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase

class UndoLastRule(): ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        ruleService.undoLastRuleSession()
        return "rule undone"
    }
}