package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Renames a comment or derived attribute. Renaming changes the attribute's
 * name only, so it is not rule building: no rule session is started and it is
 * allowed whether or not a session is in progress.
 */
data class RenameAttribute(
    val attributeName: String,
    val newName: String
) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        val summary = ruleService.renameAttribute(attributeName, newName)
        // A rename during a rule session changes the name shown for the pending
        // change, so push the status to refresh the Comments panel.
        if (ruleService.isRuleSessionActive()) ruleService.sendCornerstoneStatus()
        ChatResponse(summary)
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not rename the attribute.")
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not rename the attribute.")
    }
}
