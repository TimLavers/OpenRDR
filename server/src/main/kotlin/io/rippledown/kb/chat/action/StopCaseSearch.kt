package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Stops the search currently running on the KB, keeping the cases found so
 * far. No case needs to be selected.
 */
class StopCaseSearch : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        ChatResponse(ruleService.stopCaseSearch())
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not stop the search.")
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not stop the search.")
    }
}
