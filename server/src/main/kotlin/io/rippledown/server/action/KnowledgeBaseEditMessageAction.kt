package io.rippledown.server.action

import io.rippledown.model.ServerChatResult
import io.rippledown.server.ServerChatActionsInterface
import kotlinx.serialization.json.JsonObject

class KnowledgeBaseEditMessageAction: ServerAction {
    val userInstruction: String
    constructor(jsonObject: JsonObject) {
        userInstruction = jsonObject["userInstruction"]?.toString()?.trim('"') ?: ""
    }

    override fun doIt(application: ServerChatActionsInterface, kbId: String?): ServerChatResult {
        val result =  if (kbId != null) application.passUserMessageToKbChat(userInstruction, kbId) else "Instruction not understood."
        return ServerChatResult(result)
    }
}
