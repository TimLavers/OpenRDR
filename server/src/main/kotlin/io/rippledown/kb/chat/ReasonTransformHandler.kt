package io.rippledown.kb.chat

import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.chat.ReasonTransformer
import io.rippledown.constants.chat.EXEMPT_CORNERSTONE
import io.rippledown.toJsonString

class ReasonTransformHandler(
    private val reasonTransformer: ReasonTransformer,
    private val ruleService: RuleService,
) : FunctionCallHandler {
    override suspend fun handle(args: Map<String, Any?>): String {
        val reason = args[REASON_PARAMETER]?.toString() ?: ""
        // Some models route a bare confirmation to allow a cornerstone change (e.g. "allow") into the
        // reason-transform function instead of emitting the ExemptCornerstone action. "allow" is never a
        // valid condition, so when a cornerstone review is pending we redirect the model to the action.
        if (isAllowConfirmation(reason) && ruleService.cornerstoneStatus().numberOfCornerstones > 0) {
            return ALLOW_CORNERSTONE_CORRECTION
        }
        val transformation = reasonTransformer.transform(reason)
        val result = "'$reason' evaluation: ${transformation.toJsonString()}"
        val cornerstoneStatus = transformation.cornerstoneStatusJson
        return if (cornerstoneStatus != null) "$result\nCornerstone status: $cornerstoneStatus" else result
    }

    companion object {
        // Allow-specific phrases only ("allow"), deliberately excluding generic confirmations such as
        // "yes"/"ok" which are used elsewhere in the conversation and could be misinterpreted here.
        // NOTE: only works for English of course
        private val ALLOW_CONFIRMATIONS = setOf(
            "allow",
            "allow it",
            "allow the change",
            "allow the report change",
            "allow this change",
        )

        const val ALLOW_CORNERSTONE_CORRECTION =
            "The user's reply is a confirmation to allow the current cornerstone case report change, not a " +
                    "reason. Do NOT transform it as a condition. Your VERY NEXT response MUST be a single JSON " +
                    "object {\"action\": \"$EXEMPT_CORNERSTONE\"} and nothing else - no prose, no apology."

        fun isAllowConfirmation(reason: String) =
            reason.trim().trimEnd('.', '!').lowercase() in ALLOW_CONFIRMATIONS
    }
}
