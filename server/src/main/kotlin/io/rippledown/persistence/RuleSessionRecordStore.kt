package io.rippledown.persistence

import io.rippledown.model.rule.RuleSessionRecord

interface RuleSessionRecordStore {
    fun lastAdded(): RuleSessionRecord? = all().lastOrNull()
    fun all(): List<RuleSessionRecord>
    fun store(record: RuleSessionRecord): RuleSessionRecord
    fun deleteLastAdded()
    fun load(data: List<RuleSessionRecord>)
}