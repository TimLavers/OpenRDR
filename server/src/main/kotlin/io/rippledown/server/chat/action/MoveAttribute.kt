package io.rippledown.server.chat.action

import io.rippledown.server.chat.ModelResponder
import io.rippledown.server.chat.KbEditInterface
import io.rippledown.model.caseview.ViewableCase

data class MoveAttribute(val attributeMoved: String, val destination: String): ChatAction() {
    override suspend fun doIt(
        ruleService: KbEditInterface,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): String {
        ruleService.moveAttributeTo(attributeMoved, destination)
        return "attribute moved"
    }
}