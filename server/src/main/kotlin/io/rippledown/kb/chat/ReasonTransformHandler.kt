package io.rippledown.kb.chat

import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.chat.ReasonTransformer
import io.rippledown.constants.chat.COMMIT_RULE
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
        // The instructions tell the model to transform ANY text entered during the reason-collection
        // phase, so it sometimes routes a bare decline ("no") here instead of moving on. A decline is
        // never a valid condition, so redirect the model to the step that follows the last reason.
        if (isDecline(reason)) {
            val cornerstoneStatus = ruleService.cornerstoneStatus()
            return if (cornerstoneStatus.numberOfCornerstones > 0) {
                declineWithCornerstonesToReviewCorrection(cornerstoneStatus.summary())
            } else {
                DECLINE_CORRECTION
            }
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

        // Bare declines only. Anything with more content in it may be a genuine reason and must be
        // transformed as usual.
        // NOTE: only works for English of course
        private val DECLINES = setOf(
            "no",
            "no thanks",
            "no thank you",
            "nope",
            "none",
            "no more",
            "no more reasons",
            "no reasons",
            "no reason",
            "decline",
            "i decline",
        )

        const val DECLINE_CORRECTION =
            "The user's reply is a decline to provide any more reasons, not a reason. Do NOT transform it " +
                    "as a condition. There are no cornerstone cases left to review, so your VERY NEXT response " +
                    "MUST be a single JSON object {\"action\": \"$COMMIT_RULE\"} and nothing else - no prose, no " +
                    "apology."

        fun declineWithCornerstonesToReviewCorrection(cornerstoneStatusSummary: String) =
            "The user's reply is a decline to provide any more reasons, not a reason. Do NOT transform it as a " +
                    "condition. Cornerstone cases remain to be reviewed ($cornerstoneStatusSummary), so follow " +
                    "the instructions \"Allowing or Disallowing the change to the Cornerstone Case report\": your " +
                    "VERY NEXT response MUST ask the user whether to allow the report change to that cornerstone " +
                    "case, naming it. Do NOT commit the rule yet."

        fun isDecline(reason: String) =
            reason.trim().trimEnd('.', '!').lowercase() in DECLINES
    }
}
