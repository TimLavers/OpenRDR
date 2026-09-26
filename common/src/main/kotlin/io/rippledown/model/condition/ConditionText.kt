package io.rippledown.model.condition

import kotlinx.serialization.Serializable

/**
 * A condition as shown to the user: its formal text and, when the user gave
 * one, the phrase they typed for it. See
 * documentation/design/comments.md.
 */
@Serializable
data class ConditionText(
    val formal: String,
    val phrase: String = ""
) {
    fun hasPhrase() = phrase.isNotBlank() && phrase != formal

    companion object {
        fun of(condition: Condition) = ConditionText(condition.asText(), condition.userExpression())
    }
}
