package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

/**
 * Starts a search of the KB's case lists for cases matching conditions, with
 * copies of the cases found put into a destination list. The parameters are
 * passed through verbatim: the server parses the condition texts, refuses
 * anything invalid, and its message is returned as the chat response. No case
 * needs to be selected.
 */
data class SearchCases(
    val searchConditions: List<String>? = null,
    val connector: String? = null,
    val limit: Int? = null,
    val searchLists: List<String>? = null,
    val destination: String? = null
) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse = try {
        ChatResponse(
            ruleService.searchCases(
                searchConditions.orEmpty(),
                connector ?: "",
                limit,
                searchLists.orEmpty(),
                destination ?: ""
            )
        )
    } catch (e: IllegalArgumentException) {
        ChatResponse(e.message ?: "Could not search for cases.")
    } catch (e: IllegalStateException) {
        ChatResponse(e.message ?: "Could not search for cases.")
    }
}
