package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition

class ReplaceComment(comment: String, val replacementComment: String, reasons: List<String>?) : RuleAction(comment, reasons) {
    override suspend fun buildRule(ruleService: RuleService, currentCase: ViewableCase, conditions: List<Condition>) {
        ruleService.buildRuleToReplaceComment(currentCase, comment, replacementComment, conditions)
    }
}