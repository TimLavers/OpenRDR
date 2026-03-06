package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

data class MoveAttribute(val attributeMoved: String, val destination: String): ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        ruleService.moveAttributeTo(attributeMoved, destination)
        return ChatResponse("attribute moved")
    }
}