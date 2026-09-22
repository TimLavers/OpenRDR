package io.rippledown.chat

import io.rippledown.chat.ReasonTransformation.Companion.OK
import io.rippledown.chat.ReasonTransformation.Companion.PREVIOUSLY_CALLED_MESSAGE
import io.rippledown.chat.ReasonTransformation.Companion.TRANSFORMATION_MESSAGE
import io.rippledown.model.condition.ConditionParsingResult
import kotlinx.serialization.Serializable

/**
 * Represents the result of transforming a user-entered reason for a report change to a formal condition.
 *
 * @property reasonId The id of the condition added, or null if the reason could not be transformed to a condition
 * @property message The user-facing message describing the transformation result.
 */
@Serializable
data class ReasonTransformation(
    val reasonId: Int? = null,
    val message: String,
    val cornerstoneStatusJson: String? = null
) {
    companion object {
        const val OK = "Ok"
        const val TRANSFORMATION_MESSAGE = "Added your reason '%s'."
        const val PREVIOUSLY_CALLED_MESSAGE = "Added your reason '%s' (you previously called this '%s')."
    }
}

/**
 * Converts a [ConditionParsingResult] to an [ReasonTransformation].
 *
 * @return An [ReasonTransformation] indicating whether the reason (user-entered condition) is valid and providing an appropriate message.
 */
fun ConditionParsingResult.toExpressionTransformation() = when {
    isFailure -> {
        ReasonTransformation(
            message = requireNotNull(errorMessage)
        )
    }

    else -> {
        val cond = requireNotNull(condition)
        val formal = cond.asText()
        val storedPhrase = cond.userExpression()
        val message = when {
            expression.isNotBlank() && expression != formal && expression != storedPhrase && storedPhrase.isNotBlank() ->
                PREVIOUSLY_CALLED_MESSAGE.format(formal, storedPhrase)

            storedPhrase != formal -> TRANSFORMATION_MESSAGE.format(formal)

            else -> OK
        }
        ReasonTransformation(cond.id(), message)
    }
}
