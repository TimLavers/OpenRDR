package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.ServerChatActionsInterface
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.server.chat.ModelResponder

abstract class ChatAction: ServerAction{
    override suspend fun applyAction(
        application: ServerChatActionsInterface,
        kbId: String?,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder?
    ): String {
        val ruleService = if (kbId != null) application.kb(kbId) else null
        if (ruleService != null ) {
            return doIt(ruleService, currentCase, modelResponder!!)
        }
        return "Action not called as no kbId was provided"
    }

    abstract suspend fun doIt(ruleService: KbEditInterface, currentCase: ViewableCase?, modelResponder: ModelResponder): String
}