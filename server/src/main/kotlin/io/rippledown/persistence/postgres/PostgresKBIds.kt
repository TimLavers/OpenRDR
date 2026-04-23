package io.rippledown.persistence.postgres

import io.rippledown.persistence.PersistentKBIds
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val KB_IDS_TABLE = "kb_ids"

class PostgresKBIds(private val db: Database): PersistentKBIds {
    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(KBIds)
        }
    }

    override fun data(): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        transaction(db) {
            KBId.all().forEach { result[it.kbId] = it.deleted }
        }
        return result
    }

    override fun add(key: String, value: Boolean) {
        transaction(db) {
            KBIds.insert {
                it[kbId] = key
                it[deleted] = value
            }
        }
    }

    override fun remove(key: String) {
        transaction(db) {
            KBIds.deleteWhere {
                kbId eq key
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