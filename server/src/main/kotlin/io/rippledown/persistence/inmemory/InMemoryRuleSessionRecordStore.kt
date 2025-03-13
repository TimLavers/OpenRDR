package io.rippledown.persistence.inmemory

import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore

class InMemoryRuleSessionRecordStore : RuleSessionRecordStore {
    private val data = mutableListOf<RuleSessionRecord>()

    override fun all(): List<RuleSessionRecord> = data.toList()

    override fun createImpl(record: RuleSessionRecord): RuleSessionRecord {
        val currentMaxId = all().maxOfOrNull { it.id?: 0 } ?: 0
        val toStore = record.copy(id = currentMaxId + 1)
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

    override fun load(data: List<RuleSessionRecord>) {
        if (all().isNotEmpty()) {
            throw IllegalArgumentException("Load should not be called if there are already items stored.")
        }
        this.data.addAll(data)
    }
}