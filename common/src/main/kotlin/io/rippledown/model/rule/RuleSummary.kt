package io.rippledown.model.rule

import io.rippledown.model.condition.Condition
import kotlinx.serialization.Serializable

@Serializable
data class RuleSummary(
    val id: Int = 0,
    val conditions: Set<Condition> = setOf(),
    val conditionTextsFromRoot: List<String> = listOf(),
    val assignment: AssignValue? = null
)