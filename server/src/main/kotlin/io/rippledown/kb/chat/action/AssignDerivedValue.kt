package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.AttributeKind
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

class AssignDerivedValue(
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

        val existingAttribute = ruleService.attributeForName(attributeName)
        if (existingAttribute != null && existingAttribute.kind != AttributeKind.EXTERNAL) {
            if (existingAttribute.name.equals(attributeName, ignoreCase = true)) {
                return ChatResponse(
                    "A derived attribute named \"${existingAttribute.name}\" already exists.\nPlease choose a different name."
                )
            }
        }

        return try {
            val cornerstoneStatus = ruleService.startRuleSessionToAssignValue(
                sessionCase,
                attributeName,
                valueExpression
            )
            ruleService.sendCornerstoneStatus()
            modelResponder.response(cornerstoneStatus.summary())
        } catch (e: IllegalStateException) {
            ChatResponse(e.message ?: "Could not start derived-value rule session.")
        }
    }
}
