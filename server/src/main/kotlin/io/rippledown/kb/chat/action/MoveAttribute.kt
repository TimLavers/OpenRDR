package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase

data class MoveAttribute(val attributeMoved: String, val destination: String): ChatAction {
    override suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?): String {
        ruleService.moveAttributeTo(attributeMoved, destination)
        return "attribute moved"
    }
}