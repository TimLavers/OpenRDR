package io.rippledown.kb.chat

import io.rippledown.constants.chat.ASSIGN_DERIVED_VALUE
import io.rippledown.constants.chat.EXEMPT_CORNERSTONE
import io.rippledown.constants.chat.USER_ACTION

/**
 * Owns the server's rule-building dialogue decisions: accepting corrected assignment offers, recognising
 * cornerstone allowances, and asking for further reasons whenever a model turn adds a condition.
 * Explicit [State] records the pending offer or question so the next reply has the correct context;
 * [RuleService] remains the authority for the rule session and cornerstone status.
 * [ChatManager] executes the returned actions and calls [reset] when a new conversation starts.
 */
class RuleConversation(private val service: RuleService?) {
    sealed interface State {
        data object Ready : State
        data class OfferedAssignment(val action: ActionComment) : State
        data object AwaitingReasonReply : State
    }

    data class Turn(val message: String, val conditionsBefore: Set<String>)

    var state: State = State.Ready
        private set

    fun reset() {
        state = State.Ready
    }

    fun cornerstoneAction(message: String): ActionComment? =
        if (service != null && service.isRuleSessionActive()
            && ReasonTransformHandler.isAllowConfirmation(message)
            && service.cornerstoneStatus().numberOfCornerstones > 0
        ) ActionComment(EXEMPT_CORNERSTONE) else null

    fun assignmentAction(message: String): ActionComment? {
        val pending = state as? State.OfferedAssignment ?: return null
        reset()
        return pending.action.takeIf { isAcceptance(message) && service?.isRuleSessionActive() != true }
    }

    fun prepareTurn(message: String): Turn {
        val status = service?.takeIf { it.isRuleSessionActive() }?.cornerstoneStatus()
        val question = if (state == State.AwaitingReasonReply && status != null) {
            "[The server asked the user: $MORE_REASONS_QUESTION]\n$message"
        } else message
        val contextualised = if (status == null) question
        else "$CURRENT_CORNERSTONE_STATUS_PREFIX${status.summary()}]\n$question"
        return Turn(contextualised, service?.currentRuleSessionConditionTexts().orEmpty().toSet())
    }

    fun completeTurn(turn: Turn): ActionComment? {
        reset()
        if (service != null && service.isRuleSessionActive()
            && service.currentRuleSessionConditionTexts().any { it !in turn.conditionsBefore }
        ) {
            state = State.AwaitingReasonReply
            return ActionComment(USER_ACTION, message = MORE_REASONS_QUESTION)
        }
        return null
    }

    fun rememberOffer(action: ActionComment) {
        if (action.action != ASSIGN_DERIVED_VALUE) return
        val name = action.attributeName ?: return
        val expression = action.valueExpression ?: return
        val offered = service?.offeredValueExpressionFor(expression) ?: return
        state = State.OfferedAssignment(
            ActionComment(ASSIGN_DERIVED_VALUE, attributeName = name, valueExpression = offered)
        )
    }

    companion object {
        const val CURRENT_CORNERSTONE_STATUS_PREFIX = "[Current cornerstone status: "
        const val MORE_REASONS_QUESTION = "Added the condition. Do you want to provide any more reasons?"
    }
}
