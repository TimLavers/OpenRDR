package io.rippledown.kb.search

import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.log.lazyLogger
import io.rippledown.model.UserId
import io.rippledown.server.KBEndpoint
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

/**
 * The running case searches, keyed by KB id: one search at a time per KB,
 * while searches on different KBs may overlap. A search holds the KB
 * directly, not the user's session, so it continues while the KB is closed or
 * switched away from, and is cancelled when the KB is deleted.
 *
 * Starting a search is refused while a search is already running on the KB or
 * while anyone is building a rule on it; [io.rippledown.kb.RuleSessionManager]
 * refuses the mirror case, of starting a rule session while a search runs.
 *
 * When a search completes, its report - how many cases were found, the lists
 * searched, the conditions in the user's and the formal wording, and the
 * destination - is given to [notifyUsers] for the users on the KB at that
 * moment; with nobody on the KB it is dropped, since the report is not kept.
 * A cancelled search reports nothing: the request to stop it has already been
 * answered in the chat.
 */
class SearchCoordinator(
    private val endpointFor: (String) -> KBEndpoint?,
    private val openKnowledgeBases: OpenKnowledgeBases,
    private val notifyUsers: (Set<UserId>, String) -> Unit = { _, _ -> },
    private val executor: Executor = ForkJoinPool.commonPool()
) {
    private val logger = lazyLogger
    private val searches = ConcurrentHashMap<String, RunningSearch>()

    /** Whether a search is running on the KB; rule session starts consult this. */
    fun isRunning(kbId: String) = searches.containsKey(kbId)

    @Synchronized
    fun start(kbId: String, spec: CaseSearchSpec) {
        check(!isRunning(kbId)) { "A search is already in progress for this knowledge base." }
        val endpoint = checkNotNull(endpointFor(kbId)) { "There is no knowledge base with id \"$kbId\"." }
        val session = endpoint.session
        check(session.usersEditing().isEmpty()) {
            "Please finish or cancel the current rule before searching for cases."
        }
        val running = RunningSearch()
        searches[kbId] = running
        executor.execute {
            try {
                val result = CaseSearcher(endpoint.kb, sessionLock(session)).search(spec, running::isCancelled)
                if (!result.terminated) {
                    val users = openKnowledgeBases.usersOn(kbId)
                    // The report is not kept: with nobody on the KB, it is dropped.
                    if (users.isNotEmpty()) notifyUsers(users, reportOf(spec, result))
                }
            } catch (e: Exception) {
                logger.error("The search on KB '$kbId' failed.", e)
            } finally {
                searches.remove(kbId, running)
                running.done.countDown()
            }
        }
    }

    /** Cancels the KB's running search, keeping the cases found so far. */
    fun cancel(kbId: String): Boolean {
        val running = searches[kbId] ?: return false
        running.cancel()
        return true
    }

    /** Cancels every running search; used when the test server resets. */
    fun cancelAll() {
        searches.values.forEach { it.cancel() }
    }

    /** Waits for the KB's search to finish; true immediately if none is running. */
    fun awaitCompletion(kbId: String, timeoutMillis: Long = 5_000): Boolean =
        searches[kbId]?.done?.await(timeoutMillis, TimeUnit.MILLISECONDS) ?: true

    private fun sessionLock(session: KBSession) = object : SearchLock {
        override fun <T> withLock(block: () -> T): T = session.locked(block)
    }

    private fun reportOf(spec: CaseSearchSpec, result: SearchResult): String {
        val found = result.foundCaseIds.size
        val cases = if (found == 1) "case" else "cases"
        val conditions = spec.conditions.joinToString("; ") { "\"${it.userText}\" (${it.condition.asText()})" }
        return "Search complete. $found $cases found. " +
            "Lists searched: ${result.searchedListNames.joinToString(", ")}. " +
            "Conditions: $conditions. " +
            "Results in: ${spec.destination}."
    }

    private class RunningSearch {
        @Volatile
        private var cancelled = false
        val done = CountDownLatch(1)

        fun isCancelled() = cancelled

        fun cancel() {
            cancelled = true
        }
    }
}
