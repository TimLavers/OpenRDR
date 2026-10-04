package io.rippledown.kb

import io.rippledown.kb.lease.ProjectLease
import io.rippledown.model.UserId
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KBSession(
    val kb: KB,
    private val webSocketManager: WebSocketManager? = null,
    clock: () -> Long = System::currentTimeMillis
) {
    private val lock = ReentrantLock()
    val lease = ProjectLease({ kb.kbInfo.name }, clock)
    val ruleSessionManager = RuleSessionManager(kb, webSocketManager, lock, lease::holder)

    /**
     * Runs [block] as the only thread touching this KB. Reads are included: interpreting
     * a case writes into the stored case. The block cannot suspend, so the lock is never
     * held across I/O. See documentation/design/concurrent_users_write_lock.md.
     */
    fun <T> locked(block: () -> T): T = lock.withLock(block)

    /**
     * Takes or renews the lease for [userId], or throws [io.rippledown.kb.lease.ProjectHeldException].
     * A holder who lost an expired lease to this call loses their rule session too.
     */
    fun hold(userId: UserId) {
        lease.hold(userId)?.let { leaseLostBy(it) }
    }

    fun release(userId: UserId) {
        if (lease.releaseIfHeldBy(userId)) leaseLostBy(userId)
    }

    private fun leaseLostBy(holder: UserId) {
        if (!ruleSessionManager.isRuleSessionActive()) return
        ruleSessionManager.cancelRuleSession()
        runBlocking { webSocketManager?.sendRuleSessionCompleted(holder) }
    }
}
