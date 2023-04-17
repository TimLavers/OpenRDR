package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import kotlinx.serialization.Serializable

@Serializable
data class RuleSummary(
    val id: String = "",
    val conclusion: Conclusion? = null,
    val conditions: Set<Condition> = setOf()
)