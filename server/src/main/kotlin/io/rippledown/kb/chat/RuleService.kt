package io.rippledown.kb.chat

import io.rippledown.model.Attribute
import io.rippledown.model.CommentVariable
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.edit.EditableCondition
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.model.rule.UndoRuleDescription

interface RuleService {
    fun startRuleSessionToAddComment(
        viewableCase: ViewableCase,
        comment: String,
        variables: List<CommentVariable> = emptyList(),
    ): CornerstoneStatus
    fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus
    fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String,
        variables: List<CommentVariable> = emptyList(),
    ): CornerstoneStatus

    /**
     * The name of the comment attribute that the session in progress will
     * assign, or null if no session is in progress or it is not about adding
     * or replacing a comment. Comments are named so that they can be referred
     * to, so the name is told to the user when the comment is accepted.
     */
    fun nameOfCommentAttributeInSession(): String?

    /**
     * Rename a KB-assigned attribute (a comment or a derived attribute),
     * which changes its name only: everything that refers to it does so by
     * id. Returns a summary of the change.
     */
    fun renameAttribute(currentName: String, newName: String): String
    fun startRuleSessionToAssignValue(
        viewableCase: ViewableCase,
        attributeName: String,
        valueExpression: String
    ): CornerstoneStatus

    /**
     * The value expression the user is being offered in place of
     * [valueExpression], or null if it raises no question. An expression naming
     * something that is no attribute is put back to the user, either as a
     * correction or as text to assign, and this is what they accept by saying
     * yes, so that the acceptance can be acted on without asking the model to
     * re-send it.
     */
    fun offeredValueExpressionFor(valueExpression: String): String?

    fun startRuleSessionToRemoveAssignment(viewableCase: ViewableCase, attributeName: String): CornerstoneStatus
    fun startRuleSessionToReplaceAssignment(
        viewableCase: ViewableCase,
        attributeName: String,
        replacementValueExpression: String
    ): CornerstoneStatus

    /**
     * Edit the stored definition of a derived attribute in place, so that
     * the change applies everywhere the attribute is given by its
     * definition, with no rule change. Returns a summary of the change.
     * See documentation/design/editing_derived_attribute_definitions.md.
     */
    fun editDerivedAttributeDefinition(attributeName: String, valueExpression: String): String

    fun exemptCornerstoneCase(): CornerstoneStatus
    fun selectCornerstoneCase(index: Int): CornerstoneStatus
    fun addConditionToCurrentRuleSession(condition: Condition)
    fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult

    /**
     * The condition an editable suggestion gives when the value the user is
     * editing is replaced by [value], or the reason it cannot be used. Built
     * here rather than parsed from text so that nothing has to transcribe the
     * condition: the model gives the suggestion and the new value, and the
     * server substitutes.
     */
    fun conditionForEditedSuggestion(
        case: RDRCase,
        editableCondition: EditableCondition,
        value: String
    ): ConditionParsingResult
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

    /**
     * Resolve an attribute by its id against the full knowledge base attribute set, independent of
     * whether the current case has a value for it, or null if there is no such attribute.
     */
    fun attributeById(id: Int): Attribute?

    /**
     * All attributes in the knowledge base, including those not present on the current case.
     */
    fun allAttributes(): Set<Attribute>

    fun copyCaseToFavourites(case: ViewableCase, newName: String?): RDRCase
    fun deleteCaseFromFavourites(case: ViewableCase)
}