package io.rippledown.kb

import io.rippledown.log.lazyLogger
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.ChangeLogStore

/**
 * The history of changes made to a knowledge base, one record per committed
 * mutation. See documentation/design/concurrent_users.md.
 */
class ChangeLog(private val store: ChangeLogStore, private val clock: () -> Long = System::currentTimeMillis) {
    private val logger = lazyLogger
    var onRecorded: (ChangeRecord) -> Unit = {}

    /**
     * The mutation this records has already happened, so a store failure is logged
     * and the unpersisted record returned rather than reporting the mutation as failed.
     */
    fun record(
        userId: UserId,
        kind: ChangeKind,
        summary: String,
        ruleIds: Set<Int> = emptySet(),
        attributeIds: Set<Int> = emptySet()
    ): ChangeRecord {
        val record = ChangeRecord(null, userId.value, clock(), kind, summary, ruleIds, attributeIds)
        val recorded = try {
            store.create(record)
        } catch (e: Exception) {
            logger.error("Could not persist change record $record", e)
            record
        }
        try {
            onRecorded(recorded)
        } catch (e: Exception) {
            logger.error("Could not notify clients of change record $recorded", e)
        }
        return recorded
    }

    fun all(): List<ChangeRecord> = store.all()

    fun mostRecent(count: Int): List<ChangeRecord> = all().takeLast(count).reversed()

    fun since(id: Int?): List<ChangeRecord> =
        if (id == null) all() else all().filter { checkNotNull(it.id) > id }

    fun concerning(attributeId: Int, limit: Int): List<ChangeRecord> =
        all().filter { attributeId in it.attributeIds }.takeLast(limit).reversed()
}
