package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService

data class MoveAttribute(val attributeMoved: String, val destination: String): ChatAction {
    override fun doIt(ruleService: RuleService): String {
        ruleService.moveAttributeTo(attributeMoved, destination)
        return "attribute moved"
    }
}