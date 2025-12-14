package io.rippledown.server

import io.rippledown.model.KBInfo

interface ServerChatActionsInterface {
    fun listKnowledgeBases(): List<KBInfo>
    fun openKB(name: String): Result<KBInfo>
    fun passUserMessageToKbChat(message: String, kbId: String): String
}