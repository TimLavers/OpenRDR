package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Deletes the current case from the user-defined list that holds it. The
 * server refuses if the case is in a built-in list, and its message is
 * returned as the chat response.
 */
class DeleteCaseFromList : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        val case = requireNotNull(currentCase) { "No case is selected." }
        ruleService.deleteCaseFromUserList(case)
        ChatResponse("case deleted")
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not delete the case.")
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not delete the case.")
    }
}
