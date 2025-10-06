package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService

interface ChatAction {
    fun doIt(ruleService: RuleService): String
}