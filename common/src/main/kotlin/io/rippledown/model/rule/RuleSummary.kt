package io.rippledown.model.rule

import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionText
import kotlinx.serialization.Serializable

@Serializable
data class RuleSummary(
    val id: Int = 0,
    val conditions: Set<Condition> = setOf(),
    val conditionsFromRoot: List<ConditionText> = listOf(),
    val assignment: AssignValue? = null
)