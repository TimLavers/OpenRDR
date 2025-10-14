package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase

interface ChatAction {
    suspend fun doIt(ruleService: RuleService, currentCase: ViewableCase?): String
}