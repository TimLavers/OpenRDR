package io.rippledown.kb.chat

import io.rippledown.chat.ReasonTransformation.Companion.OK
import io.rippledown.chat.ReasonTransformation.Companion.TRANSFORMATION_MESSAGE
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.kb.chat.RuleConversation.Companion.MORE_REASONS_QUESTION
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.CornerstoneStatus

/**
 * The outcome of applying the reasons the user gave in the same instruction as
 * the rule action. See documentation/design/chat_architecture.md.
 */
class AppliedReasons(val results: List<ConditionParsingResult>) {
    fun isEmpty() = results.isEmpty()

    fun summaryForModel(): String {
        if (results.isEmpty()) return ""
        val lines = results.map { result ->
            val condition = result.condition
            if (condition != null && !result.isFailure) "  added: ${condition.asText()}"
            else "  not understood: \"${result.expression}\" - ${result.errorMessage}"
        }
        return "$REASONS_APPLIED_PREAMBLE\n${lines.joinToString("\n")}\n$REASONS_APPLIED_INSTRUCTION"
    }

    /**
     * One line per reason, in the order given. A reason typed exactly as its
     * formal condition transforms with the message [OK], which is never shown
     * to the user; it is acknowledged as added like any other.
     */
    fun acknowledgementForUser(): String = results.joinToString("\n") { result ->
        val transformation = result.toExpressionTransformation()
        if (transformation.message == OK) TRANSFORMATION_MESSAGE.format(requireNotNull(result.condition).asText())
        else transformation.message
    }

    companion object {
        const val REASONS_APPLIED_PREAMBLE = "The system has already applied the user's reasons:"
        const val REASONS_APPLIED_INSTRUCTION =
            "Do NOT ask the user for a first reason and do NOT call transformReasonToFormalCondition for " +
                    "these. Call getSuggestedConditions now."
    }
}

/**
 * Adds each reason to the rule session just started, through the path a typed
 * reason takes. Each succeeds or fails on its own; the cornerstone status is
 * pushed once, after the last, if anything was added.
 */
fun applyReasons(ruleService: RuleService, case: RDRCase, reasons: List<String>): AppliedReasons {
    val results = reasons.map { reason ->
        val result = ruleService.conditionForExpression(case, reason)
        val condition = result.condition
        if (condition == null || result.isFailure) result
        else try {
            ruleService.addConditionToCurrentRuleSession(condition)
            result
        } catch (e: IllegalArgumentException) {
            ConditionParsingResult(null, e.message, reason)
        }
    }
    if (results.any { it.condition != null && !it.isFailure }) ruleService.sendCornerstoneStatus()
    return AppliedReasons(results)
}

/**
 * The reply to a session-starting action. Without reasons this is the model's
 * reply to the cornerstone status, as it always was. With reasons, they are
 * applied first, the model is told the outcome so that its history is right,
 * and the user is shown the server's acknowledgement and asked for more, as
 * [RuleConversation.completeTurn] does after a typed reason. It cannot be left
 * to completeTurn because the model turn made here is nested inside the one
 * that produced the action, and sees the conditions as already present.
 */
suspend fun respondAfterSessionStart(
    ruleService: RuleService,
    case: ViewableCase,
    cornerstoneStatus: CornerstoneStatus,
    reasons: List<String>,
    modelResponder: ModelResponder
): ChatResponse {
    ruleService.sendCornerstoneStatus()
    if (reasons.isEmpty()) return modelResponder.response(cornerstoneStatus.summary())
    val applied = applyReasons(ruleService, case.case, reasons)
    val summary = "${ruleService.cornerstoneStatus().summary()}\n${applied.summaryForModel()}"
    val fromModel = modelResponder.response(summary)
    return fromModel.copy(text = "${applied.acknowledgementForUser()}\n\n$MORE_REASONS_QUESTION")
}
