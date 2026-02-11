package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.ServerChatActionsInterface
import io.rippledown.server.chat.ModelResponder
import kotlinx.serialization.json.JsonObject

// todo delete this
class KnowledgeBaseEditMessageAction: ServerAction {
    val userMessage: String
    constructor(jsonObject: JsonObject) {
        userMessage = jsonObject["userMessage"]?.toString()?.trim('"') ?: ""
    }

    override suspend fun applyAction(
        application: ServerChatActionsInterface,
        kbId: String?,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder?
    ) =  "to be deleted"
}
