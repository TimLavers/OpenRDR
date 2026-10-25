package io.rippledown.persistence.inmemory

import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore

class InMemoryRuleSessionRecordStore : RuleSessionRecordStore {
    private val data = mutableListOf<RuleSessionRecord>()
    // A deleted session may still be the target of another user's undo preview.
    private var nextId = 1

    override fun all(): List<RuleSessionRecord> = data.toList()

    override fun createImpl(record: RuleSessionRecord): RuleSessionRecord {
        val toStore = record.copy(id = nextId++)
        data.add(toStore)
        return toStore
    }

    override fun deleteImpl(record: RuleSessionRecord) {
        data.remove(record)
    }

    override fun deleteLastAdded() {
        if (data.isNotEmpty()) {
            data.removeLast()
        }
    }

    override fun load(data: Set<RuleSessionRecord>) {
        if (all().isNotEmpty()) {
            throw IllegalArgumentException("Load should not be called if there are already items stored.")
        }
        this.data.addAll(data)
        nextId = maxOf(nextId, (data.maxOfOrNull { it.id ?: 0 } ?: 0) + 1)
    }
}
