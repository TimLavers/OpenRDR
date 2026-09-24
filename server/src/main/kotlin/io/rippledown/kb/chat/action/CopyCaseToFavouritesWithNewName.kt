package io.rippledown.kb.chat.action

import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse

// Temporary bridge until user-defined list actions replace the favourites
// actions: copies go to a user-defined list named "Favourites".
class CopyCaseToFavouritesWithNewName(val message: String) : ChatAction {
    override suspend fun doIt(
        ruleService: RuleService,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder
    ): ChatResponse {
        ruleService.copyCaseToList(currentCase!!, "Favourites", message)
        return ChatResponse("case copied")
    }
}
