package io.rippledown.server.websocket

import io.ktor.websocket.*
import io.rippledown.constants.chat.CASES_INFO_PREFIX
import io.rippledown.constants.chat.KB_CLOSED
import io.rippledown.constants.chat.KB_INFO_PREFIX
import io.rippledown.constants.chat.RULE_SESSION_COMPLETED
import io.rippledown.log.lazyLogger
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.toJsonString
import java.util.concurrent.ConcurrentHashMap

/**
 * The connected clients, keyed by user. One user may have several windows,
 * hence a set of sessions per user. See documentation/design/concurrent_users.md.
 */
class WebSocketManager {

    private val connections = ConcurrentHashMap<UserId, MutableSet<WebSocketSession>>()
    private val logger = lazyLogger

    suspend fun setSession(userId: UserId, session: WebSocketSession) {
        connections.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
        try {
            // Keep the session open until the client disconnects
            for (frame in session.incoming) {
                frame.readBytes()
            }
        } finally {
            connections.compute(userId) { _, sessions ->
                sessions?.apply { remove(session) }?.takeIf { it.isNotEmpty() }
            }
            session.close()
        }
    }

    fun connectedUsers(): Set<UserId> = connections.keys.toSet()

    suspend fun sendStatus(userId: UserId, status: CornerstoneStatus) {
        sendToUser(userId, status.toJsonString<CornerstoneStatus>())
    }

    suspend fun sendCasesInfo(casesInfo: CasesInfo) {
        broadcast(CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>())
    }

    suspend fun sendRuleSessionCompleted(userId: UserId) {
        sendToUser(userId, RULE_SESSION_COMPLETED)
    }

    suspend fun sendKbInfo(userId: UserId, kbInfo: KBInfo) {
        sendToUser(userId, KB_INFO_PREFIX + kbInfo.toJsonString<KBInfo>())
    }

    suspend fun sendKbClosed(userId: UserId) {
        sendToUser(userId, KB_CLOSED)
    }

    suspend fun sendToUser(userId: UserId, message: String) {
        connections[userId]?.forEach { send(it, message) }
    }

    suspend fun broadcast(message: String) {
        connections.values.forEach { sessions -> sessions.forEach { send(it, message) } }
    }

    // A dead connection must not fail the request that triggered the push.
    private suspend fun send(session: WebSocketSession, message: String) {
        try {
            session.send(message)
        } catch (e: Exception) {
            logger.warn("Could not push to a client: ${e.message}")
        }
    }
}
