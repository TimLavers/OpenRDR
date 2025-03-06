package io.rippledown.persistence.inmemory

import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore

class InMemoryRuleSessionRecordStore : RuleSessionRecordStore {
    private val data = mutableListOf<RuleSessionRecord>()

    override fun all(): List<RuleSessionRecord> = data.toList()

    override fun store(record: RuleSessionRecord): RuleSessionRecord {
        all().forEach{
            val common = record.idsOfRulesAddedInSession.intersect(it.idsOfRulesAddedInSession)
            if (common.isNotEmpty()) {
                throw IllegalArgumentException("New record shares ids with other rules: $common")
            }
        }
        val currentMaxId = all().maxOfOrNull { it.index } ?: 0
        val newRecord = RuleSessionRecord(currentMaxId + 1, record.idsOfRulesAddedInSession)
        data.add(newRecord)
        return newRecord
    }

    override fun load(data: List<RuleSessionRecord>) {
        if (this.data.isNotEmpty()) {
            throw IllegalArgumentException("Load should not be called if there are already items stored.")
        }
        this.data.addAll(data)
    }

    override fun deleteLastAdded() {
        if (data.isNotEmpty()) {
            data.removeLast()
        }
    }
}