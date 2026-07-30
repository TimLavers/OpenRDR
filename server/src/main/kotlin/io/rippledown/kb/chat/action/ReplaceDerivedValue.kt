package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class ReplaceDerivedValue(
    val attributeName: String,
    val valueExpression: String,
) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        if (ruleService.isRuleSessionActive()) {
            return ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR)
        }
        val sessionCase = currentCase ?: throw IllegalStateException("No current case")

        return try {
            val cornerstoneStatus = ruleService.startRuleSessionToReplaceAssignment(
                sessionCase,
                attributeName,
                valueExpression
            )
            ruleService.sendCornerstoneStatus()
            modelResponder.response(cornerstoneStatus.summary())
        } catch (e: IllegalStateException) {
            ChatResponse(e.message ?: "Could not replace derived value.")
        }
    }
}
