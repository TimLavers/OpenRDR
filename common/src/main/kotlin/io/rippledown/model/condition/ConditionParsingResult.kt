package io.rippledown.model.condition

import kotlinx.serialization.Serializable

@Serializable
data class ConditionParsingResult(val condition: Condition? = null, val errorMessage: String? = null)
