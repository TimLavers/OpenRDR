package io.rippledown.kb.chat

import io.rippledown.chat.ReasonTransformation
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.model.condition.ConditionParsingResult

/** Server-generated transformation messages from the current model turn, keyed by formal condition text. */
class ReasonAcknowledgements {
    private val byCondition = mutableMapOf<String, String>()

    fun clear() = byCondition.clear()

    fun record(result: ConditionParsingResult) {
        if (result.isFailure) return
        val condition = result.condition ?: return
        val message = result.toExpressionTransformation().message
        if (message != ReasonTransformation.OK) byCondition[condition.asText()] = message
    }

    fun snapshot(): Map<String, String> = byCondition.toMap()
}
