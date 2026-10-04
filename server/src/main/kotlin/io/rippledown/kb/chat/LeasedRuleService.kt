package io.rippledown.kb.chat

import io.rippledown.kb.KBSession
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription

/**
 * The chat's view of a KB's [RuleService] for one user. Every call runs under
 * the KB's lock, and every call that edits the KB takes or renews the project
 * lease first, so a KB held by someone else is refused before anything happens.
 * See documentation/design/concurrent_users.md and concurrent_users_write_lock.md.
 */
class LeasedRuleService(
    private val userId: UserId,
    private val session: KBSession,
    private val delegate: RuleService
) : RuleService {

    private fun <T> held(action: RuleService.() -> T): T = session.locked {
        session.hold(userId)
        delegate.action()
    }

    private fun <T> read(action: RuleService.() -> T): T = session.locked { delegate.action() }

    override fun nameOfCommentAttributeInSession() = read { nameOfCommentAttributeInSession() }

    override fun offeredValueExpressionFor(valueExpression: String) =
        read { offeredValueExpressionFor(valueExpression) }

    // Translates the expression, which may call the LLM: the delegate locks only
    // the part that touches the KB, so this must not be wrapped here.
    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult =
        delegate.conditionForExpression(case, expression)

    override fun conditionForEditedSuggestion(
        case: RDRCase,
        editableCondition: EditableCondition,
        value: String
    ): ConditionParsingResult = read { conditionForEditedSuggestion(case, editableCondition, value) }

    override fun descriptionOfMostRecentRule(): UndoRuleDescription = read { descriptionOfMostRecentRule() }

    override fun sendCornerstoneStatus() = read { sendCornerstoneStatus() }

    override fun sendRuleSessionCompleted() = read { sendRuleSessionCompleted() }

    override fun cornerstoneStatus(): CornerstoneStatus = read { cornerstoneStatus() }

    override fun conditionHintsForCase(case: RDRCase): ConditionList = read { conditionHintsForCase(case) }

    override fun conditionForSuggestionText(case: RDRCase, conditionText: String): Condition? =
        read { conditionForSuggestionText(case, conditionText) }

    override fun currentRuleSessionConditionTexts(): Set<String> = read { currentRuleSessionConditionTexts() }

    override fun isRuleSessionActive(): Boolean = read { isRuleSessionActive() }

    override fun attributeForName(name: String) = read { attributeForName(name) }

    override fun attributeById(id: Int) = read { attributeById(id) }

    override fun allAttributes() = read { allAttributes() }

    override fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus = held { startRuleSessionToAddComment(viewableCase, comment, variables) }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String) =
        held { startRuleSessionToRemoveComment(viewableCase, comment) }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable>
    ): CornerstoneStatus =
        held { startRuleSessionToReplaceComment(viewableCase, replacedComment, replacementComment, variables) }

    override fun renameAttribute(currentName: String, newName: String) = held { renameAttribute(currentName, newName) }

    override fun renameCondition(conditionText: String, newPhrase: String) =
        held { renameCondition(conditionText, newPhrase) }

    override fun startRuleSessionToAssignValue(
        viewableCase: ViewableCase,
        attributeName: String,
        valueExpression: String
    ) =
        held { startRuleSessionToAssignValue(viewableCase, attributeName, valueExpression) }

    override fun startRuleSessionToRemoveAssignment(viewableCase: ViewableCase, attributeName: String) =
        held { startRuleSessionToRemoveAssignment(viewableCase, attributeName) }

    override fun startRuleSessionToReplaceAssignment(
        viewableCase: ViewableCase,
        attributeName: String,
        replacementValueExpression: String
    ) = held { startRuleSessionToReplaceAssignment(viewableCase, attributeName, replacementValueExpression) }

    override fun editDerivedAttributeDefinition(attributeName: String, valueExpression: String) =
        held { editDerivedAttributeDefinition(attributeName, valueExpression) }

    override fun exemptCornerstoneCase() = held { exemptCornerstoneCase() }

    override fun selectCornerstoneCase(index: Int) = held { selectCornerstoneCase(index) }

    override fun addConditionToCurrentRuleSession(condition: Condition) =
        held { addConditionToCurrentRuleSession(condition) }

    override fun commitCurrentRuleSession() = held { commitCurrentRuleSession() }

    override fun cancelCurrentRuleSession() = held { cancelCurrentRuleSession() }

    override fun undoLastRuleSession() = held { undoLastRuleSession() }

    override fun moveAttributeTo(moved: String, destination: String) = held { moveAttributeTo(moved, destination) }

    override fun removeCondition(conditionId: Int) = held { removeCondition(conditionId) }

    override fun removeConditionByText(conditionText: String) = held { removeConditionByText(conditionText) }

    override fun copyCaseToList(case: ViewableCase, listName: String, newName: String?): RDRCase =
        held { copyCaseToList(case, listName, newName) }

    override fun deleteCaseFromUserList(case: ViewableCase) = held { deleteCaseFromUserList(case) }
}
