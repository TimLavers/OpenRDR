package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*

/**
 * Comment ordering is not significant (resolved decision 4 of
 * documentation/design/repeat_inferencing.md), so comments are shown in
 * attribute id order, which merely makes a case's report deterministic.
 */
class InterpretationViewManager {

    fun viewableInterpretation(interpretation: Interpretation, case: RDRCase): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val commentAssignments = commentAssignments(interpretation)
        val texts = commentAssignments.map { it.expression.rawText() }
        val renderedComments = commentAssignments.map { assignment ->
            assignment.render(case).copy(conditions = interpretation.conditionsForAssignment(assignment))
        }
        return ViewableInterpretation(
            interpretation,
            textGivenByRules = texts.joinToString(COMMENT_SEPARATOR),
            renderedComments = renderedComments
        )
    }

    /**
     * The comment-attribute assignments in the interpretation, in
     * attribute id order. An unresolved ByDefinition assignment
     * contributes no comment, matching interpretation, where a missing
     * definition makes no assignment. See "Phase 2 — comments become
     * derived attributes" in documentation/design/repeat_inferencing.md.
     */
    private fun commentAssignments(interpretation: Interpretation): List<AssignValue> =
        interpretation.assignments()
            .filter { it.attribute.kind == AttributeKind.COMMENT && it.expression != ByDefinition }
            .sortedBy { it.attribute.id }

    private fun AssignValue.render(case: RDRCase): RenderedComment =
        when (val expression = expression) {
            is CommentTemplate -> expression.render(case)
            else -> RenderedComment(expression.evaluate(case) ?: "")
        }
}

/**
 * The template or literal text of a comment value, with `${}` variable
 * tokens left in place, used for the text given by rules.
 */
private fun ValueExpression.rawText(): String = when (this) {
    is CommentTemplate -> text
    is Literal -> value
    else -> asText()
}