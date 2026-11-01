package io.rippledown.persistence.postgres

import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import io.rippledown.persistence.ChangeLogStore
import io.rippledown.persistence.idsFromString
import io.rippledown.persistence.idsToString
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val CHANGE_LOG_TABLE = "change_log"

class PostgresChangeLogStore(private val db: Database) : ChangeLogStore {
    init {
        transaction(db) {
            SchemaUtils.create(PGChangeRecords)
        }
    }

    override fun all() = transaction(db) {
        PGChangeRecord.all().map { deserialise(it) }.sortedBy { it.id }
    }

    override fun create(record: ChangeRecord): ChangeRecord = transaction(db) {
        val stored = PGChangeRecord.new {
            userId = record.userId
            timestamp = record.timestamp
            kind = record.kind.name
            summary = record.summary
            ruleIds = idsToString(record.ruleIds)
            attributeIds = idsToString(record.attributeIds)
        }
        deserialise(stored)
    }

    override fun load(data: Set<ChangeRecord>) {
        require(all().isEmpty()) { "Load should not be called if there are already items stored." }
        transaction(db) {
            data.forEach {
                PGChangeRecord.new(it.id) {
                    userId = it.userId
                    timestamp = it.timestamp
                    kind = it.kind.name
                    summary = it.summary
                    ruleIds = idsToString(it.ruleIds)
                    attributeIds = idsToString(it.attributeIds)
                }
            }
            // Rows inserted with explicit ids leave the serial sequence where it was,
            // so the next generated id would collide with a loaded one.
            data.maxOfOrNull { it.id ?: 0 }?.let { maxId ->
                exec("SELECT setval(pg_get_serial_sequence('$CHANGE_LOG_TABLE', 'id'), $maxId, true)")
            }
        }
    }

    private fun deserialise(record: PGChangeRecord) = ChangeRecord(
        record.id.value,
        record.userId,
        record.timestamp,
        ChangeKind.valueOf(record.kind),
        record.summary,
        idsFromString(record.ruleIds),
        idsFromString(record.attributeIds)
    )
}

object PGChangeRecords : IntIdTable(name = CHANGE_LOG_TABLE) {
    val userId = varchar("user_id", 256)
    val timestamp = long("timestamp")
    val kind = varchar("kind", 32)
    val summary = text("summary")
    val ruleIds = varchar("rule_ids", 2048)
    val attributeIds = varchar("attribute_ids", 2048)
}

class PGChangeRecord(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGChangeRecord>(PGChangeRecords)

    var userId by PGChangeRecords.userId
    var timestamp by PGChangeRecords.timestamp
    var kind by PGChangeRecords.kind
    var summary by PGChangeRecords.summary
    var ruleIds by PGChangeRecords.ruleIds
    var attributeIds by PGChangeRecords.attributeIds
}
