package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
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


    fun idsOfRulesGivingConclusion(conclusion: Conclusion): Set<Int> {
        return ruleSummaries.filter { conclusion == it.conclusion }.map { it.id }.toSet()
    }

    fun reset() {
        ruleSummaries.clear()
    }
}