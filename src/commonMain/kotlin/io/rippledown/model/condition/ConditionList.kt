package io.rippledown.model.condition

import kotlinx.serialization.Serializable

@Serializable
data class ConditionList(val conditionList: List<Condition> = emptyList())