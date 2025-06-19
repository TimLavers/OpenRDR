package io.rippledown.persistence.postgres

import io.rippledown.model.rule.RuleSessionRecord
import io.rippledown.model.rule.parseToIds
import io.rippledown.persistence.RuleSessionRecordStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val RULE_SESSIONS_TABLE = "rule_sessions"

class PostgresRuleSessionRecordStore(private val db: Database) : RuleSessionRecordStore {
    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGRuleSessionRecords)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGRuleSessionRecord.all().map { deserialise(it) }.sortedBy { it.index }
    }

    override fun createImpl(record: RuleSessionRecord): RuleSessionRecord {
        return transaction(db) {
            val stored = PGRuleSessionRecord.new {
                index = record.index
                ruleIdsStr = record.idsString()
            }
            return@transaction deserialise(stored)
        }
    }

    override fun deleteImpl(record: RuleSessionRecord) {
        transaction(db) {
            PGRuleSessionRecord.findById(record.id!!)?.delete()
        }
    }

    override fun load(data: Set<RuleSessionRecord>) {
        if (all().isNotEmpty()) {
            throw IllegalArgumentException("Load should not be called if there are already items stored.")
        }
        transaction(db) {
            data.forEach {
                PGRuleSessionRecord.new(it.id) {
                    index = it.index
                    ruleIdsStr = it.idsString()
                }
            }
        }
    }

    private fun deserialise(record: PGRuleSessionRecord) =
        RuleSessionRecord(record.id.value, record.index, parseToIds(record.ruleIdsStr))
}

object PGRuleSessionRecords : IntIdTable(name = RULE_SESSIONS_TABLE) {
    val index = integer("index")
    val ruleIdsStr = varchar("ids", 2048)
}

class PGRuleSessionRecord(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGRuleSessionRecord>(PGRuleSessionRecords)

    var index by PGRuleSessionRecords.index
    var ruleIdsStr by PGRuleSessionRecords.ruleIdsStr
}