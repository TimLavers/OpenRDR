package io.rippledown.server.action

import io.rippledown.model.ServerChatResult
import io.rippledown.server.ServerChatActionsInterface
import kotlinx.serialization.json.JsonObject

class ListKnowledgeBases: ServerAction {
    constructor(jsonObject: JsonObject)

    override fun doIt(application: ServerChatActionsInterface, kbId: String?): ServerChatResult {
        return ServerChatResult(application.kbList().sorted().joinToString("\n") { it.name })
    }
}