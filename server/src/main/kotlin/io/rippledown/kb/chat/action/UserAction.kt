package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase

data class UserAction(val message: String) : ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder) =
        message
}