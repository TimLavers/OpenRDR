package io.rippledown.kb.chat

import io.rippledown.kb.KBSession
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.UserId
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.CornerstoneStatus

/**
 * The chat's view of a KB's [RuleService] for one user: every call that edits
 * the KB takes or renews the project lease first, so a KB held by someone else
 * is refused before anything happens. Reads and pushes pass straight through.
 * See documentation/design/concurrent_users.md.
 */
class LeasedRuleService(
    private val userId: UserId,
    private val session: KBSession,
    private val delegate: RuleService
) : RuleService by delegate {

    private fun <T> held(action: RuleService.() -> T): T {
        session.hold(userId)
        return delegate.action()
    }

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
