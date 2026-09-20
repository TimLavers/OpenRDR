package io.rippledown.model.condition

import kotlinx.serialization.Serializable

@Serializable
data class ConditionParsingResult(
    val condition: Condition? = null,
    val errorMessage: String? = null,
    // The incoming wording, which may differ from the phrase on a reused condition.
    val expression: String = ""
) {
    val isFailure
        get() = errorMessage != null
}
