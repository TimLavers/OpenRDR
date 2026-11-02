package io.rippledown.kb.search

import io.rippledown.kb.KB
import io.rippledown.model.Interpretation
import io.rippledown.model.rule.IncompleteInferenceException

/**
 * Runs a block as the only thread touching the KB, as [io.rippledown.kb.KBSession.locked]
 * does. The searcher takes and releases it around each case, never across
 * the whole search, so that chat turns - including a request to stop the
 * search - can run between cases. The KB session supplies the real lock;
 * tests use [SearchLock.NONE], which does not lock at all.
 */
interface SearchLock {
    fun <T> withLock(block: () -> T): T

    companion object {
        val NONE = object : SearchLock {
            override fun <T> withLock(block: () -> T): T = block()
        }
    }
}

/**
 * Runs one search: the cases of the given lists are tried in turn, each one
 * interpreted if the spec's conditions touch a KB-assigned (comment, derived
 * or AI-derived) attribute, evaluated against the conditions, and copied to
 * the destination list as it is found - which is why the cases found before
 * cancellation are retained, and why the order of the results is not
 * guaranteed. The search stops when the limit is reached, when the lists are
 * exhausted, or when [isCancelled] reports that the user stopped it.
 *
 * Interpretation uses prepared (saved) AI evaluations only: a search never
 * initiates classification, so a case whose interpretation cannot complete
 * without an unprepared evaluation is simply not a match.
 */
class CaseSearcher(
    private val kb: KB,
    private val lock: SearchLock = SearchLock.NONE
) {

    fun search(spec: CaseSearchSpec, isCancelled: () -> Boolean = { false }): SearchResult {
        val needsInterpretation = lock.withLock { specNeedsInterpretation(spec) }
        val foundIds = mutableListOf<Long>()
        var terminated = false
        search@ for (listType in spec.sourceLists) {
            val ids = lock.withLock { kb.caseIds(listType).mapNotNull { it.id } }
            for (id in ids) {
                if (isCancelled()) {
                    terminated = true
                    break@search
                }
                val matched = lock.withLock { matchCase(id, spec, needsInterpretation) }
                if (matched) {
                    foundIds.add(id)
                    if (spec.limit != null && foundIds.size >= spec.limit) break@search
                }
            }
        }
        return SearchResult(foundIds.toList(), spec.sourceLists.map { it.name }, terminated)
    }

    private fun specNeedsInterpretation(spec: CaseSearchSpec): Boolean =
        spec.conditions.flatMap { it.condition.attributeNames() }.any { name ->
            kb.attributeManager.byName(name)?.kind?.isAssignedByKB() == true
        }

    /**
     * One case under the lock: interpret it if the spec needs interpretation,
     * evaluate the conditions, and copy a match to the destination list.
     */
    private fun matchCase(id: Long, spec: CaseSearchSpec, needsInterpretation: Boolean): Boolean {
        val raw = kb.getCase(id) ?: return false
        val candidate = if (needsInterpretation) {
            try {
                kb.materialise(raw.copy(interpretation = Interpretation(raw.caseId)))
            } catch (_: IncompleteInferenceException) {
                null
            } ?: return false
        } else {
            raw
        }
        val results = spec.conditions.map { it.condition.holds(candidate) }
        val matched = if (spec.connector == SearchConnector.ALL) results.all { it } else results.any { it }
        if (matched) kb.copyCaseToList(id, spec.destination)
        return matched
    }
}

/** What a completed or cancelled search found. */
data class SearchResult(
    val foundCaseIds: List<Long>,
    val searchedListNames: List<String>,
    val terminated: Boolean
)
