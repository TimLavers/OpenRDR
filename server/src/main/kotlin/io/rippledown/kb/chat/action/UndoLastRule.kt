package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService

class UndoLastRule(): ChatAction {
    override fun doIt(ruleService: RuleService): String {
        ruleService.undoLastRule()
        return "rule undone"
    }
}