package io.rippledown.chat

import io.rippledown.chat.ReasonTransformation.Companion.OK
import io.rippledown.chat.ReasonTransformation.Companion.TRANSFORMATION_MESSAGE
import io.rippledown.model.condition.ConditionParsingResult
import kotlinx.serialization.Serializable

/**
 * Represents the result of transforming a user-entered reason for a report change to a formal condition.
 *
 * @property isTransformed Indicates whether the reason can be transformed to a formal condition that is true for the case.
 * @property message The user-facing message describing the transformation result.
 */
@Serializable
data class ReasonTransformation(val isTransformed: Boolean, val message: String) {
    companion object {
        const val OK = "Ok"
        const val TRANSFORMATION_MESSAGE = "Your reason is equivalent to '%s'."
    }
}

/**
 * Converts a [ConditionParsingResult] to an [ReasonTransformation].
 *
 * @return An [ReasonTransformation] indicating whether the reason (user-entered condition) is valid and providing an appropriate message.
 */
fun ConditionParsingResult.toExpressionTransformation() = when {
    isFailure -> {
        ReasonTransformation(false, requireNotNull(errorMessage))
    }

    else -> {
        val transformed = requireNotNull(condition)
        val message = when {
            transformed.userExpression() != transformed.asText() ->
                TRANSFORMATION_MESSAGE.format(transformed.asText())

            else -> OK
        }
        ReasonTransformation(true, message)
    }
}