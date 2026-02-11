package io.rippledown.server.chat.action

import io.rippledown.constants.chat.CHAT_BOT_DONE_MESSAGE
import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase

class CommitRule : ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        ruleService.commitCurrentRuleSession()
        ruleService.sendRuleSessionCompleted()
        return CHAT_BOT_DONE_MESSAGE
    }
}