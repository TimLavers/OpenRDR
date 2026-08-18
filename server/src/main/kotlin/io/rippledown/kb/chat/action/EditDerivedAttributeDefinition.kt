package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Edits the stored definition of a derived attribute in place, applying the
 * change everywhere the attribute is given by its definition. Unlike
 * [AssignDerivedValue] and [ReplaceDerivedValue], this does not open a rule
 * session — no rule is built and there is no cornerstone review. See
 * documentation/design/editing_derived_attribute_definitions.md.
 */
class EditDerivedAttributeDefinition(
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
        return try {
            ChatResponse(ruleService.editDerivedAttributeDefinition(attributeName, valueExpression))
        } catch (e: IllegalStateException) {
            ChatResponse(e.message ?: "Could not edit the definition.")
        }
    }
}
