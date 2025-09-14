package io.rippledown.kb.chat

import io.rippledown.log.lazyLogger
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.*

class ChatRuleService(
    private val getOrCreateConclusion: (String) -> Conclusion,
    private val startCornerstoneReviewSession: (RDRCase, RuleTreeChange) -> CornerstoneStatus,
    private val cornerstoneReviewSessionStarted: () -> Boolean = { false },
    private val addCondition: (Condition) -> Unit,
    private val conditionForExpression: (String, RDRCase) -> ConditionParsingResult,
    private val undoLastRuleOnKB: () -> Unit,
    private val commitRuleSession: () -> Unit
) : RuleService {
    private val logger = lazyLogger

    override fun undoLastRule() = undoLastRuleOnKB()

    override fun startCornerstoneReviewSessionToAddComment(case: RDRCase, comment: String): CornerstoneStatus {
        val conclusion = getOrCreateConclusion(comment)
        val action = ChangeTreeToAddConclusion(conclusion)
        return startCornerstoneReviewSession(case, action)
    }

    override fun startCornerstoneReviewSessionToRemoveComment(case: RDRCase, comment: String): CornerstoneStatus {
        val conclusion = getOrCreateConclusion(comment)
        val action = ChangeTreeToRemoveConclusion(conclusion)
        return startCornerstoneReviewSession(case, action)
    }

    override fun startCornerstoneReviewSessionToReplaceComment(
        case: RDRCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus {
        val replacedConclusion = getOrCreateConclusion(replacedComment)
        val replacementConclusion = getOrCreateConclusion(replacementComment)
        val action = ChangeTreeToReplaceConclusion(replacedConclusion, replacementConclusion)
        return startCornerstoneReviewSession(case, action)
    }

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
        if (!cornerstoneReviewSessionStarted()) {
            startCornerstoneReviewSession(case, action)
        }
        conditions.forEach { addCondition(it) }
        commitRuleSession()
    }

    override suspend fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        return conditionForExpression(expression, case)
    }
}