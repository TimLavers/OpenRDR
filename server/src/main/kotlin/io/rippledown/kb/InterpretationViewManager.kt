package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*

interface ConclusionProvider : EntityProvider<Conclusion> {
    fun getOrCreate(text: String, variables: List<CommentVariable>): Conclusion
}

/**
 * Comment ordering is not significant (resolved decision 4 of
 * documentation/design/repeat_inferencing.md), so comments are shown in
 * id order, which merely makes a case's report deterministic. The
 * conclusions of a knowledge base that has not yet been converted to
 * comment attributes are shown ahead of any assignments.
 */
class InterpretationViewManager(
    private val attributeProvider: EntityProvider<io.rippledown.model.Attribute>
) {

    fun viewableInterpretation(interpretation: Interpretation, case: RDRCase): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val orderedConclusions = interpretation.conclusions().sortedBy { it.id }
        val renderedFromConclusions = orderedConclusions.map { conclusion ->
            conclusion.render(case) { id ->
                runCatching { attributeProvider.getById(id) }.getOrNull()
            }.copy(conditions = interpretation.conditionsForConclusion(conclusion))
        }
        val commentAssignments = commentAssignments(interpretation)
        val texts = orderedConclusions.map { it.text } + commentAssignments.map { it.expression.rawText() }
        val renderedComments = renderedFromConclusions + commentAssignments.map { assignment ->
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
 * tokens left in place — the raw counterpart of [Conclusion.text], used
 * for the text given by rules.
 */
private fun ValueExpression.rawText(): String = when (this) {
    is CommentTemplate -> text
    is Literal -> value
    else -> asText()
}