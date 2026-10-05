package io.rippledown.kb.chat

import io.rippledown.kb.KBSession
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription

/**
 * The chat's view of a KB's [RuleService]: every call runs under the KB's lock.
 * See documentation/design/concurrent_users_write_lock.md.
 */
class LockedRuleService(
    private val session: KBSession,
    private val delegate: RuleService
) : RuleService {

    private fun <T> locked(action: RuleService.() -> T): T = session.locked { delegate.action() }

    override fun nameOfCommentAttributeInSession() = locked { nameOfCommentAttributeInSession() }

    override fun offeredValueExpressionFor(valueExpression: String) =
        locked { offeredValueExpressionFor(valueExpression) }

    // Translates the expression, which may call the LLM: the delegate locks only
    // the part that touches the KB, so this must not be wrapped here.
    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult =
        delegate.conditionForExpression(case, expression)

    override fun conditionForEditedSuggestion(
        case: RDRCase,
        editableCondition: EditableCondition,
        value: String
    ): ConditionParsingResult = locked { conditionForEditedSuggestion(case, editableCondition, value) }

    override fun descriptionOfMostRecentRule(): UndoRuleDescription = locked { descriptionOfMostRecentRule() }

    override fun sendCornerstoneStatus() = locked { sendCornerstoneStatus() }

    override fun sendRuleSessionCompleted() = locked { sendRuleSessionCompleted() }

    override fun cornerstoneStatus(): CornerstoneStatus = locked { cornerstoneStatus() }

    override fun conditionHintsForCase(case: RDRCase): ConditionList = locked { conditionHintsForCase(case) }

    override fun conditionForSuggestionText(case: RDRCase, conditionText: String): Condition? =
        locked { conditionForSuggestionText(case, conditionText) }

    override fun currentRuleSessionConditionTexts(): Set<String> = locked { currentRuleSessionConditionTexts() }

    override fun isRuleSessionActive(): Boolean = locked { isRuleSessionActive() }

    override fun attributeForName(name: String) = locked { attributeForName(name) }

    override fun attributeById(id: Int) = locked { attributeById(id) }

    override fun allAttributes() = locked { allAttributes() }

    override fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus = locked { startRuleSessionToAddComment(viewableCase, comment, variables) }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String) =
        locked { startRuleSessionToRemoveComment(viewableCase, comment) }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus =
        locked { startRuleSessionToReplaceComment(viewableCase, replacedComment, replacementComment, variables) }

    override fun renameAttribute(currentName: String, newName: String) =
        locked { renameAttribute(currentName, newName) }

    override fun renameCondition(conditionText: String, newPhrase: String) =
        locked { renameCondition(conditionText, newPhrase) }

    override fun startRuleSessionToAssignValue(
        viewableCase: ViewableCase,
        attributeName: String,
        valueExpression: String
    ) =
        locked { startRuleSessionToAssignValue(viewableCase, attributeName, valueExpression) }

    override fun startRuleSessionToRemoveAssignment(viewableCase: ViewableCase, attributeName: String) =
        locked { startRuleSessionToRemoveAssignment(viewableCase, attributeName) }

    override fun startRuleSessionToReplaceAssignment(
        viewableCase: ViewableCase,
        attributeName: String,
        replacementValueExpression: String
    ) = locked { startRuleSessionToReplaceAssignment(viewableCase, attributeName, replacementValueExpression) }

    override fun editDerivedAttributeDefinition(attributeName: String, valueExpression: String) =
        locked { editDerivedAttributeDefinition(attributeName, valueExpression) }

    override fun exemptCornerstoneCase() = locked { exemptCornerstoneCase() }

    override fun selectCornerstoneCase(index: Int) = locked { selectCornerstoneCase(index) }

    override fun addConditionToCurrentRuleSession(condition: Condition) =
        locked { addConditionToCurrentRuleSession(condition) }

    override fun commitCurrentRuleSession() = locked { commitCurrentRuleSession() }

    override fun cancelCurrentRuleSession() = locked { cancelCurrentRuleSession() }

    override fun undoLastRuleSession() = locked { undoLastRuleSession() }

    override fun moveAttributeTo(moved: String, destination: String) = locked { moveAttributeTo(moved, destination) }

    override fun removeCondition(conditionId: Int) = locked { removeCondition(conditionId) }

    override fun removeConditionByText(conditionText: String) = locked { removeConditionByText(conditionText) }

    override fun copyCaseToList(case: ViewableCase, listName: String, newName: String?): RDRCase =
        locked { copyCaseToList(case, listName, newName) }

    override fun deleteCaseFromUserList(case: ViewableCase) = locked { deleteCaseFromUserList(case) }
}
