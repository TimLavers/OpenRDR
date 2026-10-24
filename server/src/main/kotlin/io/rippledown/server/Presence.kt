package io.rippledown.server

import io.rippledown.constants.chat.PRESENCE_PREFIX
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.PresenceInfo
import io.rippledown.model.PresentUser
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.toJsonString

/**
 * Who is on each knowledge base: the users whose conversation is about it and
 * who have a window connected. Recomputed from those two sources on every
 * push, so nothing here can go stale. See concurrent_users.md.
 */
class Presence(
    private val openKnowledgeBases: OpenKnowledgeBases,
    private val webSocketManager: WebSocketManager,
    private val kbSessionFor: (String) -> KBSession?
) {
    fun present(kbId: String): List<PresentUser> {
        val connected = openKnowledgeBases.usersOn(kbId) intersect webSocketManager.connectedUsers()
        val editing = kbSessionFor(kbId)?.usersEditing().orEmpty()
        return connected.sortedBy { it.value }.map { PresentUser(it.value, it in editing) }
    }

    fun push(kbId: String) {
        webSocketManager.broadcast(PRESENCE_PREFIX + PresenceInfo(kbId, present(kbId)).toJsonString<PresenceInfo>())
    }

    fun transition(fromKbId: String?, toKbId: String?) {
        setOfNotNull(fromKbId, toKbId).forEach(::push)
    }
}
