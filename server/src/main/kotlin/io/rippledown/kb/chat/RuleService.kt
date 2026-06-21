package io.rippledown.kb.chat

import io.rippledown.model.Attribute
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription

interface RuleService {
    fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable> = emptyList()
    ): CornerstoneStatus
    fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable> = emptyList()
    ): CornerstoneStatus
    fun exemptCornerstoneCase(): CornerstoneStatus
    fun selectCornerstoneCase(index: Int): CornerstoneStatus
    fun addConditionToCurrentRuleSession(condition: Condition)
    fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult
    fun commitCurrentRuleSession()
    fun cancelCurrentRuleSession()
    fun undoLastRuleSession()
    fun descriptionOfMostRecentRule(): UndoRuleDescription
    fun moveAttributeTo(moved: String, destination: String)
    fun sendCornerstoneStatus()
    fun sendRuleSessionCompleted()
    fun removeCondition(conditionId: Int): CornerstoneStatus
    fun removeConditionByText(conditionText: String): CornerstoneStatus
    fun cornerstoneStatus(): CornerstoneStatus
    fun conditionHintsForCase(case: RDRCase): ConditionList
    fun conditionForSuggestionText(case: RDRCase, conditionText: String): Condition?
    fun currentRuleSessionConditionTexts(): Set<String>
    fun isRuleSessionActive(): Boolean

    /**
     * Resolve a (possibly misspelt or differently-cased) attribute name typed or dictated by the user
     * to a known attribute, or null if there is no acceptable match.
     */
    fun attributeForName(name: String): Attribute?
}