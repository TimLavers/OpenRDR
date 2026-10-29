package io.rippledown.persistence.inmemory

import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.ChangeLogStore

class InMemoryChangeLogStore : ChangeLogStore {
    private val data = mutableListOf<ChangeRecord>()

    override fun all(): List<ChangeRecord> = data.sortedBy { it.id }

    override fun create(record: ChangeRecord): ChangeRecord {
        val currentMaxId = data.maxOfOrNull { it.id ?: 0 } ?: 0
        val toStore = record.copy(id = currentMaxId + 1)
        data.add(toStore)
        return toStore
    }

    override fun load(data: Set<ChangeRecord>) {
        require(this.data.isEmpty()) { "Load should not be called if there are already items stored." }
        this.data.addAll(data)
    }
}
