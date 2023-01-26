package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.Serializable

@Serializable
data class Interpretation(val caseId: CaseId = CaseId(), val text: String = "") {
    private val ruleSummaries = mutableSetOf<RuleSummary>()

    fun textGivenByRules(): String {
        return ruleSummaries.map { it.conclusion?.text }
            .filterNotNull()
            .toMutableSet()//eliminate duplicates
            .toMutableList()
            .sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString("\n")
    }

    fun add(ruleSummary: RuleSummary) {
        ruleSummaries.add(ruleSummary)
    }

    fun add(rule: Rule) {
        ruleSummaries.add(rule.summary())
    }

    fun conclusions(): Set<Conclusion> {
        return ruleSummaries.mapNotNull { it.conclusion }.toSet()
    }

    fun conditionsForConclusion(conclusion: Conclusion): List<String> {
        return ruleSummaries.filter { ruleSummary -> conclusion == ruleSummary.conclusion }
            .flatMap { conclusion -> conclusion.conditions }
            .map { condition -> condition.asText() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun idsOfRulesGivingConclusion(conclusion: Conclusion): Set<String> {
        return ruleSummaries.filter { conclusion == it.conclusion }.map { it.id }.toSet()
    }

    fun ruleSummaries(): Set<RuleSummary> {
        return ruleSummaries.toSet()
    }

    fun reset() {
        ruleSummaries.clear()
    }
}