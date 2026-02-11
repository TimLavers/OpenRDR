package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.ServerChatActionsInterface
import io.rippledown.server.chat.ModelResponder
import kotlinx.serialization.json.JsonObject

class ListKnowledgeBases: ServerAction {
    constructor()

    override suspend fun applyAction(
        application: ServerChatActionsInterface,
        kbId: String?,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder?)= application.kbList().sorted().joinToString("\n") { it.name }
}