package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class CancelRule : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        ruleService.cancelCurrentRuleSession()
        ruleService.sendRuleSessionCompleted()
        return ChatResponse(RULE_CANCELLED_MESSAGE)
    }

    companion object {
        const val RULE_CANCELLED_MESSAGE = "The rule has been cancelled."
    }
}
