package io.rippledown.persistence

import io.rippledown.model.rule.RuleSessionRecord

interface RuleSessionRecordStore {
    fun lastAdded(): RuleSessionRecord? = all().lastOrNull()

    fun all(): List<RuleSessionRecord>

    fun create(record: RuleSessionRecord): RuleSessionRecord {
        val currentMaxIndex = all().maxOfOrNull { it.index } ?: 0
        val newRecord = RuleSessionRecord(null,currentMaxIndex + 1, record.idsOfRulesAddedInSession)
        return createImpl(newRecord)
    }

    fun createImpl(record: RuleSessionRecord): RuleSessionRecord

    fun deleteImpl(record: RuleSessionRecord)

    fun deleteLastAdded() {
        val lastAdded = lastAdded()
        if (lastAdded != null) {
            deleteImpl(lastAdded)
        }
    }

    fun load(data: List<RuleSessionRecord>)
}