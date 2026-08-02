package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*
import io.rippledown.persistence.OrderStore

interface ConclusionProvider : EntityProvider<Conclusion> {
    fun getOrCreate(text: String, variables: List<CommentVariable>): Conclusion
}

class InterpretationViewManager(
    conclusionOrderStore: OrderStore,
    conclusionProvider: ConclusionProvider,
    private val attributeProvider: EntityProvider<io.rippledown.model.Attribute>
) :
    OrderedEntityManager<Conclusion>(conclusionOrderStore, conclusionProvider) {

    fun viewableInterpretation(interpretation: Interpretation, case: RDRCase): ViewableInterpretation {
        require(interpretation.caseId.id != null) {
            "Cannot create a viewable interpretation if the case does not have an id."
        }
        val orderedConclusions = inOrder(interpretation.conclusions())
        val renderedFromConclusions = orderedConclusions.map { conclusion ->
            conclusion.render(case) { id ->
                runCatching { attributeProvider.getById(id) }.getOrNull()
            }
        }
        val commentAssignments = commentAssignments(interpretation)
        val texts = orderedConclusions.map { it.text } + commentAssignments.map { it.expression.rawText() }
        val renderedComments = renderedFromConclusions + commentAssignments.map { it.render(case) }
        return ViewableInterpretation(
            interpretation,
            textGivenByRules = texts.joinToString(COMMENT_SEPARATOR),
            renderedComments = renderedComments
        )
    }

    /**
     * The comment-attribute assignments in the interpretation, in
     * attribute id order (comment ordering is not significant; this makes
     * the report deterministic). An unresolved ByDefinition assignment
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