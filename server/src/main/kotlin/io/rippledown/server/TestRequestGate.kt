package io.rippledown.server

import io.rippledown.model.UserId
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Installed only on explicitly opted-in test servers. Never hold [lock] across application work or I/O. */
class TestRequestGate(private val drainTimeoutMillis: Long = 5_000) {
    private enum class State { ACCEPTING, RESETTING, UNUSABLE }

    private val lock = Any()
    private var state = State.ACCEPTING
    private var activeRequests = 0
    private var generation = 0L

    init {
        require(drainTimeoutMillis > 0)
    }

    fun enterRequest(): Boolean = admitRequest() != null

    fun admitRequest(): Long? = synchronized(lock) {
        if (state != State.ACCEPTING) null else {
            activeRequests++
            generation
        }
    }

    fun leaveRequest() = synchronized(lock) {
        check(activeRequests > 0) { "No admitted request to finish" }
        activeRequests--
    }

    // Registration must be atomic with closing admission, including upgrades already in progress.
    fun registerConnection(expectedGeneration: Long? = null, register: () -> Unit): Boolean = synchronized(lock) {
        if (state != State.ACCEPTING || (expectedGeneration != null && generation != expectedGeneration)) false else {
            register()
            true
        }
    }

    suspend fun reset(connectedUsers: () -> Set<UserId>, clear: suspend () -> Unit) {
        synchronized(lock) {
            if (state != State.ACCEPTING) throw ResetRejectedException("Server is resetting or requires replacement")
            state = State.RESETTING
        }
        var succeeded = false
        try {
            val drained = withTimeoutOrNull(drainTimeoutMillis) {
                while (requestCount() != 0 || connectedUsers().isNotEmpty()) delay(10)
                true
            } == true
            if (!drained) throw ResetRejectedException(
                "Reset timed out: ${requestCount()} active request(s), connected users: ${connectedUsers()}"
            )
            check(requestCount() == 0 && connectedUsers().isEmpty())
            clear()
            succeeded = true
        } finally {
            synchronized(lock) {
                if (succeeded) generation++
                state = if (succeeded) State.ACCEPTING else State.UNUSABLE
            }
        }
    }

    private fun requestCount(): Int = synchronized(lock) { activeRequests }
}

class ResetRejectedException(message: String) : IllegalStateException(message)
