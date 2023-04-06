package io.rippledown.persistence.postgres

import io.rippledown.model.KBInfo
import io.rippledown.persistence.PersistentKB
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val KB_INFO_TABLE = "kb_info"

fun createPostgresKb(dbName: String, kbInfo: KBInfo): PostgresKB {
    Database.connect({ConnectionProvider.connection(dbName)})
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(PKBInfos)
        PKBInfo.new{
            kbId = kbInfo.id
            name = kbInfo.name
        }
        commit()
    }
    return PostgresKB(dbName)
}

class PostgresKB internal constructor(private val dbName: String): PersistentKB {

    init {
        Database.connect({ConnectionProvider.connection(dbName)})
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PKBInfos)
            commit()
        }
    }

    override fun kbInfo(): KBInfo {
        val resultList = mutableListOf<KBInfo>()
        transaction {
            PKBInfo.all().forEach {
                resultList.add(KBInfo(it.kbId, it.name))
            }
        }
        require(resultList.size == 1) {
            "Should be precisely one KBInfo, found $resultList"
        }
        return resultList[0]
    }
}
object PKBInfos: LongIdTable(name = KB_INFO_TABLE) {
    val kbId = varchar("kb_id", 128)
    val name = varchar("name", 128)
}
class PKBInfo(id: EntityID<Long>): LongEntity(id){
    companion object: LongEntityClass<PKBInfo>(PKBInfos)
    var kbId by PKBInfos.kbId
    var name by PKBInfos.name
}