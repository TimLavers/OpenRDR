package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
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

    fun conclusions(): Set<Conclusion> {
        return ruleSummaries.mapNotNull { it.conclusion }.toSet()
    }

    fun conclusionTexts(): Set<String> {
        return conclusions().map { it.text }.toSet()
    }

    fun toComments(case: RDRCase, attributeById: (Int) -> Attribute? = { null }): String {
        return conclusions().map { conclusion ->
            if (conclusion.variables.isEmpty()) {
                conclusion.text
            } else {
                // Convert internal ${} placeholders back to {attributeName} format for LLM.
                // Resolve the attribute name from the full attribute set first (an attribute may be
                // valid for the knowledge base even if the current case has no value for it), falling
                // back to the case's attributes and finally to "unknown".
                var result = conclusion.text
                var position = 0
                for (variable in conclusion.variables) {
                    val tokenIndex = result.indexOf(VARIABLE_TOKEN, position)
                    if (tokenIndex != -1) {
                        val attribute = attributeById(variable.attributeId)
                            ?: case.attributes.find { it.id == variable.attributeId }
                        val attributeName = attribute?.name ?: "unknown"
                        result = result.replaceRange(tokenIndex, tokenIndex + VARIABLE_TOKEN.length, "{$attributeName}")
                        position = tokenIndex + attributeName.length + 2 // +2 for {}
                    }
                }
                result
            }
        }.toSet().toJsonString()
    }

    fun idsOfRulesGivingConclusion(conclusion: Conclusion): Set<Int> {
        return ruleSummaries.filter { conclusion == it.conclusion }.map { it.id }.toSet()
    }

    fun reset() {
        ruleSummaries.clear()
    }

    fun conditionsForConclusion(conclusion: Conclusion): List<String> {
        return ruleSummaries
            .first { ruleSummary -> conclusion == ruleSummary.conclusion }
            .conditionTextsFromRoot
    }
}