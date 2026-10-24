package io.rippledown.kb

import io.rippledown.model.UserId
import io.rippledown.server.websocket.WebSocketManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * @param sessionStateChanged called whenever a user's rule session starts or ends,
 * so that who is editing can be re-announced. See concurrent_users.md.
 */
class KBSession(
    val kb: KB,
    private val webSocketManager: WebSocketManager? = null,
    private val sessionStateChanged: () -> Unit = {}
) {
    private val lock = ReentrantLock()
    private val ruleSessionManagers = ConcurrentHashMap<UserId, RuleSessionManager>()

    /**
     * The rule-building session of [userId] on this KB. Each user has their own
     * session state; the engine operations all act on the one [kb] under [locked].
     * See documentation/design/concurrent_users.md.
     */
    fun ruleSessionManagerFor(userId: UserId): RuleSessionManager =
        ruleSessionManagers.computeIfAbsent(userId) {
            RuleSessionManager(kb, webSocketManager, lock, it, sessionStateChanged)
        }

    /**
     * Runs [block] as the only thread touching this KB. Reads are included: interpreting
     * a case writes into the stored case. The block cannot suspend, so the lock is never
     * held across I/O. See documentation/design/concurrent_users.md.
     */
    fun <T> locked(block: () -> T): T = lock.withLock(block)

    /**
     * The users with a rule session in progress on this KB. Deleting the KB
     * is refused while anyone else is among them.
     * See documentation/design/concurrent_users.md.
     */
    fun usersEditing(): Set<UserId> = locked {
        ruleSessionManagers.filterValues { it.isRuleSessionActive() }.keys
    }

    /**
     * Cancels [userId]'s rule session, if any, and tells them, so their client
     * drops its session state. Used when the user closes the KB.
     */
    fun cancelRuleSessionOf(userId: UserId) = locked {
        val ruleSessionManager = ruleSessionManagers[userId] ?: return@locked
        if (!ruleSessionManager.isRuleSessionActive()) return@locked
        ruleSessionManager.cancelRuleSession()
        webSocketManager?.sendRuleSessionCompleted(userId)
    }
}
