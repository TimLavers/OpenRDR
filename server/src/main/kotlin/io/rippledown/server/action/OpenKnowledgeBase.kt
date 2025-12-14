package io.rippledown.server.action

import io.rippledown.model.ServerChatResult
import io.rippledown.server.ServerChatActionsInterface
import kotlinx.serialization.json.JsonObject

class OpenKnowledgeBase: ServerAction {
    val kbName: String
    constructor(jsonObject: JsonObject) {
        kbName = jsonObject["kbName"]?.toString()?.trim('"') ?: ""
    }

    override fun doIt(application: ServerChatActionsInterface, kbId: String?): ServerChatResult {
        val result = application.openKB(kbName)
        return if (result.isSuccess) {
            ServerChatResult("${result.getOrThrow().name} has been opened", result.getOrThrow())
        } else {
            ServerChatResult(result.exceptionOrNull()!!.message!!)
        }
    }
}
