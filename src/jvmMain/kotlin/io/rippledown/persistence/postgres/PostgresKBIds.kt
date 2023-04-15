package io.rippledown.persistence.postgres

import io.rippledown.persistence.PersistentKBIds
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

const val KB_IDS_TABLE = "kb_ids"

class PostgresKBIds(private val dbName: String): PersistentKBIds {

    init {
        Database.connect({ConnectionProvider.connection(dbName)})
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(KBIds)
        }
    }

    override fun data(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        transaction {
            KBId.all().forEach { result[it.kbId] = it.deleted }
        }
        return result
    }

    override fun add(key: String, value: Boolean) {
        transaction {
            KBIds.insert {
                it[kbId] = key
                it[deleted] = value
            }
        }
    }
}
object KBIds: LongIdTable(name = KB_IDS_TABLE) {
    val kbId = varchar("kb_id", 128)
    val deleted = bool("deleted")
}
class KBId(id: EntityID<Long>): LongEntity(id){
    companion object: LongEntityClass<KBId>(KBIds)
    var kbId by KBIds.kbId
    var deleted by KBIds.deleted
}