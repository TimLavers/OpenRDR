package io.rippledown.server.websocket

import io.ktor.websocket.*
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.UserId
import io.rippledown.model.chat.ChatContextInfo
import io.rippledown.model.rule.CornerstoneStatus
import io.rippledown.server.TestRequestGate
import io.rippledown.toJsonString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

const val SEND_TIMEOUT_MILLIS = 5_000L

/**
 * The connected clients, keyed by user. One user may have several windows,
 * hence a set of sessions per user.
 *
 * Pushes are queued and delivered by one consumer coroutine, so a caller can
 * push from under the KB lock without holding it across I/O, and a client that
 * has stopped reading cannot stall the request that triggered the push.
 * See documentation/design/concurrent_users.md.
 */
class WebSocketManager(
    private val testRequestGate: TestRequestGate? = null,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val sendTimeoutMillis: Long = SEND_TIMEOUT_MILLIS
) {
    // A null message is a drain marker: it carries only the deferred to complete.
    private class Outgoing(val targets: Set<UserId>?, val message: String?, val done: CompletableDeferred<Unit>? = null)

    private val connections = ConcurrentHashMap<UserId, MutableSet<WebSocketSession>>()
    private val outgoing = Channel<Outgoing>(Channel.UNLIMITED)
    private val logger = lazyLogger

    /** Called with the user whose set of connections just changed, once the change is visible. */
    var connectionsChanged: (UserId) -> Unit = {}

    init {
        scope.launch {
            for (item in outgoing) {
                item.message?.let { deliver(item.targets, it) }
                item.done?.complete(Unit)
            }
        }
    }

    suspend fun setSession(userId: UserId, session: WebSocketSession, requestGeneration: Long? = null) {
        // One atomic step: a set returned by computeIfAbsent could be removed by a
        // disconnecting window's compute (below) before the add, orphaning this session.
        val register = {
            connections.compute(userId) { _, sessions ->
                (sessions ?: ConcurrentHashMap.newKeySet()).apply { add(session) }
            }
            Unit
        }
        val admitted = testRequestGate?.registerConnection(requestGeneration, register) ?: run { register(); true }
        if (!admitted) {
            session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Test server reset"))
            return
        }
        connectionsChanged(userId)
        try {
            // Keep the session open until the client disconnects
            for (frame in session.incoming) {
                frame.readBytes()
            }
        } finally {
            connections.compute(userId) { _, sessions ->
                sessions?.apply { remove(session) }?.takeIf { it.isNotEmpty() }
            }
            connectionsChanged(userId)
            session.close()
        }
    }

    fun connectedUsers(): Set<UserId> = connections.keys.toSet()

    fun sendStatus(userId: UserId, status: CornerstoneStatus) {
        sendToUser(userId, status.toJsonString<CornerstoneStatus>())
    }

    fun sendCasesInfo(casesInfo: CasesInfo) {
        broadcast(CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>())
    }

    fun sendRuleSessionCompleted(userId: UserId) {
        sendToUser(userId, RULE_SESSION_COMPLETED)
    }

    fun sendKbInfo(userId: UserId, kbInfo: KBInfo) {
        sendToUser(userId, KB_INFO_PREFIX + kbInfo.toJsonString<KBInfo>())
    }

    fun sendKbClosed(userId: UserId) {
        sendToUser(userId, KB_CLOSED)
    }

    fun sendChatContext(userId: UserId, context: ChatContextInfo) {
        sendToUser(userId, CHAT_CONTEXT_PREFIX + context.toJsonString<ChatContextInfo>())
    }

    fun sendToUser(userId: UserId, message: String) {
        outgoing.trySend(Outgoing(setOf(userId), message))
    }

    fun broadcast(message: String) {
        outgoing.trySend(Outgoing(null, message))
    }

    /**
     * Completes once everything queued before the call has been delivered or
     * given up on. For tests and the test-server reset.
     */
    suspend fun drain() {
        val done = CompletableDeferred<Unit>()
        outgoing.trySend(Outgoing(null, null, done))
        done.await()
    }

    private suspend fun deliver(targets: Set<UserId>?, message: String) {
        val sessions = if (targets == null) connections.values.flatten() else targets.flatMap { connections[it].orEmpty() }
        sessions.forEach { send(it, message) }
    }

    // A dead or stalled connection must not fail the request that triggered the push.
    private suspend fun send(session: WebSocketSession, message: String) {
        try {
            val sent = withTimeoutOrNull(sendTimeoutMillis) { session.send(message) }
            if (sent == null) logger.warn("Gave up pushing to a client after ${sendTimeoutMillis}ms.")
        } catch (e: Exception) {
            logger.warn("Could not push to a client: ${e.message}")
        }
    }
}
