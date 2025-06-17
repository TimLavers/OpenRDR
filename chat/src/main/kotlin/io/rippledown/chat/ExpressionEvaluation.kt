package io.rippledown.chat

import io.rippledown.chat.ExpressionEvaluation.Companion.VALID_CONDITION_MESSAGE
import io.rippledown.chat.ExpressionEvaluation.Companion.VALID_CONDITION_WITH_EVALUATION_MESSAGE
import io.rippledown.model.condition.ConditionParsingResult
import kotlinx.serialization.Serializable

/**
 * Represents the result of evaluating a condition expression.
 *
 * @property isValid Indicates whether the expression is valid.
 * @property message The user-facing message describing the evaluation result.
 */
@Serializable
data class ExpressionEvaluation(val isValid: Boolean, val message: String) {
    companion object {
        const val VALID_CONDITION_MESSAGE = "Your condition is valid."
        const val VALID_CONDITION_WITH_EVALUATION_MESSAGE = "Your condition is valid and will be evaluated as '%s'."
    }
}

/**
 * Converts a [ConditionParsingResult] to an [ExpressionEvaluation].
 *
 * @return An [ExpressionEvaluation] indicating whether the condition is valid and providing an appropriate message.
 * @throws IllegalStateException if the parsing result is in an inconsistent state.
 */
fun ConditionParsingResult.toExpressionEvaluation() = when {
    isFailure -> {
        ExpressionEvaluation(false, requireNotNull(errorMessage))
    }

    else -> {
        val condition = requireNotNull(condition)
        val message = when {
            condition.userExpression() != condition.asText() ->
                VALID_CONDITION_WITH_EVALUATION_MESSAGE.format(condition.asText())

            else -> VALID_CONDITION_MESSAGE
        }
        ExpressionEvaluation(true, message)
    }
}