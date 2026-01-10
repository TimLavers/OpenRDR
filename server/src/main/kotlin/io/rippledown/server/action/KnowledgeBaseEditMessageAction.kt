package io.rippledown.server.action

import io.rippledown.model.ServerChatResult
import io.rippledown.server.ServerChatActionsInterface
import kotlinx.serialization.json.JsonObject

class KnowledgeBaseEditMessageAction: ServerAction {
    val userMessage: String
    constructor(jsonObject: JsonObject) {
        userMessage = jsonObject["userMessage"]?.toString()?.trim('"') ?: ""
    }

    override fun doIt(application: ServerChatActionsInterface, kbId: String?): ServerChatResult {
        val result =  if (kbId != null) application.passUserMessageToKbChat(userMessage, kbId) else "Instruction not understood."
        return ServerChatResult(result)
    }
}
