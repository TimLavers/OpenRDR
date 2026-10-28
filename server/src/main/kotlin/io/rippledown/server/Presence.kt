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
 * push. Snapshot capture and enqueueing share the KB lock, so an older
 * calculation cannot publish after a newer one. See concurrent_users.md.
 */
class Presence(
    private val openKnowledgeBases: OpenKnowledgeBases,
    private val webSocketManager: WebSocketManager,
    private val kbSessionFor: (String) -> KBSession?
) {
    fun present(kbId: String): List<PresentUser> = withPresence(kbId) { it }

    fun push(kbId: String) = withPresence(kbId) { users ->
        webSocketManager.broadcast(PRESENCE_PREFIX + PresenceInfo(kbId, users).toJsonString<PresenceInfo>())
    }

    private fun <T> withPresence(kbId: String, action: (List<PresentUser>) -> T): T {
        val session = kbSessionFor(kbId)
        val snapshot = {
            val connected = openKnowledgeBases.usersOn(kbId) intersect webSocketManager.connectedUsers()
            val editing = session?.usersEditing().orEmpty()
            action(connected.sortedBy { it.value }.map { PresentUser(it.value, it in editing) })
        }
        // Callers may already hold this reentrant KB lock. Introducing another
        // lock here would invert that order for connection/disconnection callbacks.
        return if (session == null) snapshot() else session.locked(snapshot)
    }

    fun transition(fromKbId: String?, toKbId: String?) {
        setOfNotNull(fromKbId, toKbId).forEach(::push)
    }
}
