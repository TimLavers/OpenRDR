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

        // If the attribute already has a value for this case, the user may have
        // meant to replace it. Ask rather than guess, so that they are never
        // shown a change they did not ask for.
        val existingValue = sessionCase.derivedValues()
            .firstOrNull { it.name.equals(attributeName, ignoreCase = true) }
        if (existingValue != null) {
            return ChatResponse(
                alreadyAssignedForCaseMessage(existingValue.name, existingValue.value, valueExpression)
            )
        }

        val existingAttribute = ruleService.attributeForName(attributeName)
        if (existingAttribute != null && existingAttribute.kind != AttributeKind.EXTERNAL) {
            if (existingAttribute.name.equals(attributeName, ignoreCase = true)) {
                return ChatResponse(
                    nameClashWithExistingDerivedAttributeMessage(attributeName)
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

fun nameClashWithExistingDerivedAttributeMessage(existingAttributeName: String): String =
    "A derived attribute named \"${existingAttributeName}\" already exists.\nPlease choose a different name."

/**
 * Asked when the user requests an assignment to a derived attribute that already
 * has a value for the current case. Naming the current value and the expression
 * requested lets the user answer with a simple yes.
 */
fun alreadyAssignedForCaseMessage(
    attributeName: String,
    currentValue: String,
    requestedExpression: String
): String =
    "\"$attributeName\" is already given for this case, with the value \"$currentValue\".\n" +
            "Do you want to replace it with \"$requestedExpression\"?"

fun nameClashWithExistingExternalAttributeMessage(existingAttributeName: String): String =
    "An externally supplied attribute named \"${existingAttributeName}\" already exists.\nPlease choose a different name."

/**
 * Asked when a value expression names some attributes but one of its names is
 * no attribute at all, and something close enough to suggest exists. Building
 * the corrected formula unasked could give the user a formula they never
 * wrote, so the correction is put to them to accept with a plain yes.
 */
fun didYouMeanFormulaMessage(unknownName: String, correctedExpression: String): String =
    "There is no attribute named \"$unknownName\".\nDid you mean \"$correctedExpression\"?"

/**
 * Asked in the same situation as [didYouMeanFormulaMessage] when no attribute
 * name is close enough to suggest. The alternative reading is that the text was
 * never a formula, so that is what is offered — silently assigning it as text
 * would leave the user with a nonsense value and no hint as to why their
 * formula never evaluated.
 */
fun unknownAttributeInFormulaMessage(unknownName: String, expression: String): String =
    "There is no attribute named \"$unknownName\".\nDo you want to assign the text \"$expression\"?"
