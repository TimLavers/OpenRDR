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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val KB_INFO_TABLE = "kb_info"
val logger: Logger = LoggerFactory.getLogger("rdr")

fun createPostgresKB(kbInfo: KBInfo): PostgresKB {
    logger.info("Creating PostgresKB with KBInfo: $kbInfo")
    createDatabase(kbInfo.id)
    logger.info("Database created. About to connect.")
    val db = Database.connect({ConnectionProvider.connection(kbInfo.id)})
    transaction(db) {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(PKBInfos)
        logger.info("KBInfos table created.")
        PKBInfo.new{
            kbId = kbInfo.id
            name = kbInfo.name
        }
        logger.info("KBInfo stored.")
    }
    return PostgresKB(kbInfo.id)
}

class PostgresKB internal constructor(private val dbName: String): PersistentKB {
    private val db: Database = Database.connect({ConnectionProvider.connection(dbName)})
    private val attributeStore = PostgresAttributeStore(db)
    private val attributeOrderStore = PostgresAttributeOrderStore(db)
    private val conclusionStore = PostgresConclusionStore(db)
    private val conditionStore = PostgresConditionStore(db)
    private val ruleStore = PostgresRuleStore(db)
    private val caseStore = PostgresCaseStore(db)

    override fun kbInfo(): KBInfo {
        val resultList = mutableListOf<KBInfo>()
        transaction(db) {
            PKBInfo.all().forEach {
                resultList.add(KBInfo(it.kbId, it.name))
            }
        }
        require(resultList.size == 1) {
            "Should be precisely one KBInfo, found $resultList"
        }
        return resultList[0]
    }

    override fun attributeStore() = attributeStore

    override fun attributeOrderStore() = attributeOrderStore

    override fun conclusionStore() = conclusionStore

    override fun conditionStore() = conditionStore

    override fun ruleStore() = ruleStore

    override fun caseStore() = caseStore
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