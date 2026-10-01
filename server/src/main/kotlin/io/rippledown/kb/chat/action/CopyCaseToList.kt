package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Copies the current case to the user-defined list named by the user,
 * creating the list if it does not yet exist. The list name is passed
 * through verbatim: the server refuses reserved names, and its message
 * is returned as the chat response.
 */
data class CopyCaseToList(
    val listName: String,
    val newName: String? = null
) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        val case = requireNotNull(currentCase) { "No case is selected." }
        ruleService.copyCaseToList(case, listName, newName)
        ChatResponse("case copied")
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not copy the case.")
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not copy the case.")
    }
}
