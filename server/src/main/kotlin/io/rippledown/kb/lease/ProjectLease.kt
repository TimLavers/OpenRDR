package io.rippledown.kb.lease

import io.rippledown.model.UserId

const val LEASE_EXPIRY_MS = 10 * 60 * 1000L

fun projectHeldMessage(kbName: String, holder: UserId) = "$kbName is being edited by $holder."

class ProjectHeldException(val kbName: String, val holder: UserId) :
    RuntimeException(projectHeldMessage(kbName, holder))

/**
 * The exclusive right to edit one knowledge base. Taken on the first guarded
 * action, renewed on every one, and lost to the next claimant after
 * [LEASE_EXPIRY_MS] without activity. See documentation/design/concurrent_users.md.
 */
class ProjectLease(
    private val kbName: () -> String,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Held(val userId: UserId, val lastActivity: Long)

    private var held: Held? = null

    /**
     * Takes or renews the lease for [userId]. Returns the holder who lost an
     * expired lease to this call, or null if nobody did.
     */
    @Synchronized
    fun hold(userId: UserId): UserId? {
        val now = clock()
        val current = held
        val previous = when {
            current == null || current.userId == userId -> null
            now - current.lastActivity >= LEASE_EXPIRY_MS -> current.userId
            else -> throw ProjectHeldException(kbName(), current.userId)
        }
        held = Held(userId, now)
        return previous
    }

    @Synchronized
    fun holder(): UserId? = held?.userId

    @Synchronized
    fun release(): UserId? {
        val previous = held?.userId
        held = null
        return previous
    }

    @Synchronized
    fun releaseIfHeldBy(userId: UserId): Boolean {
        if (held?.userId != userId) return false
        held = null
        return true
    }
}
