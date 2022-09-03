package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Interpretation(val caseId: CaseId, val text: String = "") {
    private val ruleSummaries = mutableSetOf<RuleSummary>()

    @Transient
    private val rules = mutableSetOf<Rule>()

    fun add(rule: Rule) {
        ruleSummaries.add(rule.summary())
        rules.add(rule)
    }

    fun conclusions(): Set<Conclusion> {
        return ruleSummaries.mapNotNull { it.conclusion }.toSet()
    }

    fun rulesGivingConclusion(conclusion: Conclusion): Set<Rule> {
        return rules.filter {  conclusion == it.conclusion }.toSet()
    }
}