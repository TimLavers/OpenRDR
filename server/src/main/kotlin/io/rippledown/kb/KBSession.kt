package io.rippledown.kb

import io.rippledown.kb.lease.ProjectLease
import io.rippledown.model.UserId
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking

class KBSession(
    val kb: KB,
    private val webSocketManager: WebSocketManager? = null,
    clock: () -> Long = System::currentTimeMillis
) {
    val lease = ProjectLease({ kb.kbInfo.name }, clock)
    val ruleSessionManager = RuleSessionManager(kb, webSocketManager, lease::holder)

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
