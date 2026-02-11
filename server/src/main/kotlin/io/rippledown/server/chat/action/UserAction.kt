package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.server.chat.ModelResponder

data class UserAction(val message: String) : ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ) = message
}