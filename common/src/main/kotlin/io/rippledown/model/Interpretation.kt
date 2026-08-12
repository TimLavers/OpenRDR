package io.rippledown.model

import io.rippledown.model.rule.*
import io.rippledown.toJsonString
import kotlinx.serialization.Serializable

const val COMMENT_SEPARATOR = " "

@Serializable
data class Interpretation(val caseId: CaseId = CaseId()) {
    val ruleSummaries = mutableSetOf<RuleSummary>()

    fun add(ruleSummary: RuleSummary) {
        ruleSummaries.add(ruleSummary)
    }

    fun add(rule: Rule) {
        ruleSummaries.add(rule.summary())
    }

    /**
     * The derived-attribute assignments made by the rules that fired.
     */
    fun assignments(): Set<AssignValue> {
        return ruleSummaries.mapNotNull { it.assignment }.toSet()
    }

    fun idsOfRulesAssigning(attribute: Attribute): Set<Int> {
        return ruleSummaries.filter { it.assignment?.attribute == attribute }.map { it.id }.toSet()
    }

    fun idsOfRulesMakingAssignment(assignment: AssignValue): Set<Int> {
        return ruleSummaries.filter { assignment == it.assignment }.map { it.id }.toSet()
    }

    /**
     * All comment texts produced by the rules that fired, in attribute id
     * order. A [CommentTemplate] assignment contributes its template text
     * with variables in `{attributeName}` format; a [Literal] assignment
     * contributes its value; an unresolved [ByDefinition] assignment contributes
     * nothing. The [attributeById] resolver is used to render variable names.
     */
    fun commentTexts(case: RDRCase, attributeById: (Int) -> Attribute? = { null }): Set<String> =
        assignments()
            .filter { it.attribute.kind == AttributeKind.COMMENT }
            .sortedBy { it.attribute.id }
            .mapNotNull { assignment ->
                when (val expression = assignment.expression) {
                    is CommentTemplate -> expression.textWithVariableNames { id ->
                        attributeById(id) ?: case.attributes.find { it.id == id }
                    }

                    is Literal -> expression.value
                    else -> null
                }
            }
            .toSet()

    fun toComments(case: RDRCase, attributeById: (Int) -> Attribute? = { null }): String =
        commentTexts(case, attributeById).toJsonString()

    fun reset() {
        ruleSummaries.clear()
    }

    /**
     * Replaces each [ByDefinition] assignment expression with the attribute's
     * stored definition (as resolved by [resolver]), in place. Assignments
     * whose attribute has no stored definition are left unchanged. This makes
     * [commentTexts] and [toComments] work on interpretations that were
     * populated by rule-tree application, which records [ByDefinition]
     * sentinels rather than the concrete expressions.
     */
    fun resolveDefinitions(resolver: (Attribute) -> io.rippledown.model.rule.ValueExpression?) {
        val toReplace = ruleSummaries.filter { summary ->
            val assignment = summary.assignment
            assignment != null && assignment.expression is io.rippledown.model.rule.ByDefinition
        }
        toReplace.forEach { summary ->
            val assignment = summary.assignment ?: return@forEach
            val resolved = resolver(assignment.attribute) ?: return@forEach
            ruleSummaries.remove(summary)
            ruleSummaries.add(summary.copy(assignment = AssignValue(assignment.attribute, resolved)))
        }
    }

    /**
     * The condition texts from root for the rule that assigned the given
     * [AssignValue] to its attribute, or an empty list if no such rule fired.
     */
    fun conditionsForAssignment(assignment: AssignValue): List<String> {
        return ruleSummaries
            .firstOrNull { ruleSummary -> assignment == ruleSummary.assignment }
            ?.conditionTextsFromRoot
            ?: emptyList()
    }
}