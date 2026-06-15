package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

interface ChatAction {
    suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?, modelResponder: ModelResponder): ChatResponse

    companion object {
        const val RULE_SESSION_ALREADY_ACTIVE_ERROR =
            "Please finish or cancel the current rule before starting a new one."
    }
}