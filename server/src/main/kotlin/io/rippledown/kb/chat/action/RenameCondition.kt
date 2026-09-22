package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/** Renames shared display wording without starting or finishing a rule session. */
data class RenameCondition(val conditionText: String, val newPhrase: String) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        val summary = ruleService.renameCondition(conditionText, newPhrase)
        if (ruleService.isRuleSessionActive()) ruleService.sendCornerstoneStatus()
        ChatResponse(summary)
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not rename the condition.")
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not rename the condition.")
    }
}
