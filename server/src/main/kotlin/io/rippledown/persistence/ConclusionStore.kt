package io.rippledown.persistence

import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion

interface ConclusionStore {
    fun all(): Set<Conclusion>
    fun create(text: String): Conclusion
    fun create(text: String, variables: List<CommentVariable>): Conclusion
    fun store(conclusion: Conclusion)
    fun load(conclusions: Set<Conclusion>)

    /**
     * Remove all conclusions. Used by the conclusion migration: migrated
     * KBs have an empty conclusion store, making the migration idempotent.
     * See "Phase 2" in documentation/design/repeat_inferencing.md.
     */
    fun clear()
}