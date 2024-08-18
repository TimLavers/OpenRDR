package io.rippledown.model.condition

import io.rippledown.model.condition.edit.SuggestedCondition
import kotlinx.serialization.Serializable

@Serializable
data class ConditionList(val suggestions: List<SuggestedCondition> = emptyList()) {
    val conditions: List<Condition> = suggestions.map { it.initialSuggestion() }
}

// Not sure if we need this or if List<Condition> is better
@Serializable
data class RuleConditionList(val conditions: List<Condition> = emptyList())