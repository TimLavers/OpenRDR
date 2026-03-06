package io.rippledown.kb.chat.action

import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class CommitRule : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        ruleService.commitCurrentRuleSession()
        ruleService.sendRuleSessionCompleted()
        return ChatResponse(CHAT_BOT_DONE_MESSAGE)
    }
}