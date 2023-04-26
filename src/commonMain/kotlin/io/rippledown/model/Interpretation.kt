package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.Serializable

@Serializable
data class Interpretation(val caseId: CaseId = CaseId(), var verifiedText: String? = null) {
    private val ruleSummaries = mutableSetOf<RuleSummary>()

    fun latestText() = if (verifiedText != null) verifiedText else textGivenByRules()

    fun textGivenByRules(): String {
        return ruleSummaries.asSequence().map { it.conclusion?.text }
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
        return ruleSummaries.first { ruleSummary -> conclusion == ruleSummary.conclusion }
            .conditions.map { condition -> condition.asText() }
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