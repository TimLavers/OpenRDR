package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

// Temporary bridge until user-defined list actions replace the favourites
// actions: deletes the current case from whichever user-defined list holds it.
class DeleteCaseFromFavourites() : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        ruleService.deleteCaseFromUserList(currentCase!!)
        return ChatResponse("case deleted")
    }
}
