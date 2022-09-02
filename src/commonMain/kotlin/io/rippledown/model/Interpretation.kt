package io.rippledown.model

import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleSummary
import kotlinx.serialization.Serializable

@Serializable
data class Interpretation(val caseId: CaseId, val text: String) {
    private val rules = mutableSetOf<RuleSummary>()

    fun add(rule: Rule) {
        rules.add(rule.summary())
    }

    fun conclusions(): Set<Conclusion> {
        return rules.mapNotNull { it.conclusion }.toSet()
    }
}