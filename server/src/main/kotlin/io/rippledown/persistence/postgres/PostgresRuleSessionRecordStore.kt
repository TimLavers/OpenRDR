package io.rippledown.persistence.postgres

import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.persistence.RuleSessionRecordStore
import org.jetbrains.exposed.sql.Database

class PostgresRuleSessionRecordStore(private val db: Database) : RuleSessionRecordStore {

    override fun all(): List<RuleSessionRecord> {
        TODO("Not yet implemented")
    }

    override fun store(record: RuleSessionRecord): RuleSessionRecord {
        TODO("Not yet implemented")
    }

    override fun deleteLastAdded() {
        TODO("Not yet implemented")
    }

    override fun load(data: List<RuleSessionRecord>) {
        TODO("Not yet implemented")
    }
}