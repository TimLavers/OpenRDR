package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*

/**
 * Comments are currently shown in attribute-id order. This makes their order
 * deterministic, but it is not persisted user-controlled ordering; that work
 * is not implemented.
 */
class InterpretationViewManager {

    fun viewableInterpretation(interpretation: Interpretation, case: RDRCase): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val commentAssignments = commentAssignments(interpretation)
        val texts = commentAssignments.map { it.expression.rawText() }
        val renderedComments = commentAssignments.map { assignment ->
            assignment.render(case).copy(
                conditions = interpretation.conditionsForAssignment(assignment),
                name = assignment.attribute.name,
                attributeId = assignment.attribute.id
            )
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
     * definition makes no assignment.
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
