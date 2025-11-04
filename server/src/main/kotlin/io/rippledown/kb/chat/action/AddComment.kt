package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition

class AddComment(comment: String, reasons: List<String>?) : RuleAction(comment, reasons) {
    override suspend fun buildRule(ruleService: RuleService, currentCase: ViewableCase, conditions: List<Condition>) {
        ruleService.buildRuleToAddComment(currentCase, comment, conditions)
    }
}