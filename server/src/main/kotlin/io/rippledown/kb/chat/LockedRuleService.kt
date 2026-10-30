package io.rippledown.kb.chat

import io.rippledown.kb.KBSession
import io.rippledown.kb.ai.AiInferenceCoordinator
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription
import kotlinx.coroutines.runBlocking
import io.rippledown.model.rule.AiBooleanExample

/**
 * The chat's view of a KB's [RuleService]: every call runs under the KB's lock,
 * and any AI evaluation it needs is obtained with the lock released. The
 * interface is synchronous, so the calling thread waits for the evaluation;
 * the lock is never held during it. See documentation/design/concurrent_users.md.
 */
class LockedRuleService(
    private val session: KBSession,
    private val delegate: RuleService,
    private val coordinator: AiInferenceCoordinator
) : RuleService {

    override fun proposeAiDefinition(
        attributeName: String, sourceName: String, instructions: String, examples: List<AiBooleanExample>,
        caseId: Long?, reasons: List<String>, edit: Boolean
    ) = locked { proposeAiDefinition(attributeName, sourceName, instructions, examples, caseId, reasons, edit) }

    override fun currentAiDefinitionDraft() = locked { currentAiDefinitionDraft() }
    override fun currentAiSourceCorrection() = locked { currentAiSourceCorrection() }
    override fun previewAiDefinition(draftId: String, case: RDRCase?, retryAfterFailure: Boolean) =
        locked(retryAfterFailure) { previewAiDefinition(draftId, case) }
    override fun acceptAiDefinition(draftId: String, case: RDRCase?) = locked { acceptAiDefinition(draftId, case) }
    override fun cancelAiDefinitionDraft() = locked { cancelAiDefinitionDraft() }
    override fun explainAiEvaluation(attributeName: String, case: RDRCase?) =
        locked(savedOnly = true) { explainAiEvaluation(attributeName, case) }
    override fun retryAiEvaluation(case: RDRCase) = locked(retryAfterFailure = true) { retryAiEvaluation(case) }

    private fun <T> locked(retryAfterFailure: Boolean = false, savedOnly: Boolean = false, action: RuleService.() -> T): T {
        var guard: (() -> Unit)? = null
        return runBlocking {
            val operation = {
                session.locked {
                    val currentGuard = guard ?: delegate.operationGuard().also { guard = it }
                    currentGuard()
                    delegate.action()
                }
            }
            if (savedOnly) coordinator.withSavedEvaluations(null, operation)
            else coordinator.withPreparedEvaluations(null, retryAfterFailure, operation)
        }
    }

    override fun nameOfCommentAttributeInSession() = locked { nameOfCommentAttributeInSession() }

    override fun offeredValueExpressionFor(valueExpression: String) =
        locked { offeredValueExpressionFor(valueExpression) }

    // Translates the expression, which may call the LLM: the delegate locks only
    // the part that touches the KB, so this must not be wrapped here.
    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        val guard = session.locked { delegate.operationGuard() }
        return runBlocking { coordinator.withPreparedEvaluations(case.id) {
            session.locked { guard() }
            val result = delegate.conditionForExpression(case, expression)
            session.locked { guard(); result }
        } }
    }

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
