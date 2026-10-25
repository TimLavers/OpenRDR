package io.rippledown.persistence

import io.rippledown.model.rule.evaluation.AiEvaluationKey
import io.rippledown.model.rule.evaluation.AiEvaluationRecord

/**
 * The successful AI classifications of one knowledge base, keyed by
 * [AiEvaluationKey]. A result is authoritative only once it is here, and
 * the first record stored for a key is the one every caller uses, so
 * duplicate requests that race cannot produce different values. Records
 * are never expired; the KB's deletion removes them with its other data.
 * See documentation/tickets/ai_derived_attributes_design.md.
 */
interface AiEvaluationStore {
    fun recordFor(key: AiEvaluationKey): AiEvaluationRecord?

    /**
     * Store [record] unless a record with its key exists, and return the
     * record now stored under that key: [record] itself if it won, else
     * the existing one. Atomic with respect to other callers.
     */
    fun insertIfAbsent(record: AiEvaluationRecord): AiEvaluationRecord
}
