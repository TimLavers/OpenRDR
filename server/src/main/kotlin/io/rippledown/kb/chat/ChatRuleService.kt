package io.rippledown.kb.chat

import io.rippledown.log.lazyLogger
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.model.rule.RuleTreeChange

class ChatRuleService(
    private val getOrCreateConclusion: (String) -> Conclusion,
    private val startRuleSession: (RDRCase, RuleTreeChange) -> Unit,
    private val addCondition: (Condition) -> Unit,
    private val commitRuleSession: () -> Unit,
    private val conditionForExpression: (String, RDRCase) -> ConditionParsingResult
) : RuleService {
    private val logger = lazyLogger

    override suspend fun buildRuleToAddComment(case: RDRCase, comment: String, conditions: List<Condition>) {
        val conclusion = getOrCreateConclusion(comment)
        val action = ChangeTreeToAddConclusion(conclusion)
        buildRule(case, action, conditions)
    }

    override suspend fun buildRuleToRemoveComment(case: RDRCase, comment: String, conditions: List<Condition>) {
        val conclusion = getOrCreateConclusion(comment)
        val action = ChangeTreeToRemoveConclusion(conclusion)
        buildRule(case, action, conditions)
    }

    override suspend fun buildRuleToReplaceComment(
        case: RDRCase,
        replacedComment: String,
        replacementComment: String,
        conditions: List<Condition>
    ) {
        val replacedConclusion = getOrCreateConclusion(replacedComment)
        val replacementConclusion = getOrCreateConclusion(replacementComment)
        val action = ChangeTreeToReplaceConclusion(replacedConclusion, replacementConclusion)
        buildRule(case, action, conditions)
    }

    private fun buildRule(
        case: RDRCase,
        action: RuleTreeChange,
        conditions: List<Condition>
    ) {
        startRuleSession(case, action)
        conditions.forEach { addCondition(it) }
        commitRuleSession()
    }

    override suspend fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        return conditionForExpression(expression, case)
    }
}