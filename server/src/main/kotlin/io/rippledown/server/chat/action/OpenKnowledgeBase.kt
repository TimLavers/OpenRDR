package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.ServerChatActionsInterface
import io.rippledown.server.chat.ModelResponder
import kotlinx.serialization.json.JsonObject

class OpenKnowledgeBase(val kbName: String) : ServerAction {

    constructor(jsonObject: JsonObject) : this(jsonObject["kbName"]?.toString()?.trim('"') ?: "")

    override suspend fun applyAction(
        application: ServerChatActionsInterface,
        kbId: String?,
        currentCase: ViewableCase?,
        modelResponder: ModelResponder?
    ): String {
        val result = application.openKB(kbName)
        return if (result.isSuccess) {
            "${result.getOrThrow().name} has been opened"
        } else {
            result.exceptionOrNull()!!.message!!
        }
    }
}
