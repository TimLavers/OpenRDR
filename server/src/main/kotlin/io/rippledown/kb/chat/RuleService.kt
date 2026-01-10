package io.rippledown.kb.chat

import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.CornerstoneStatus

interface RuleService {
    fun startRuleSessionToAddComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus
    fun exemptCornerstoneCase(): CornerstoneStatus
    fun addConditionToCurrentRuleSession(condition: Condition)
    fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult
    fun commitCurrentRuleSession()
    fun undoLastRuleSession()
    fun moveAttributeTo(moved: String, destination: String)
    suspend fun sendCornerstoneStatus()
    suspend fun sendRuleSessionCompleted()
}