package io.rippledown.persistence.postgres

import io.rippledown.persistence.AttributeOrderStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val ATTRIBUTE_INDEXES_TABLE = "attribute_indexes"

class PostgresAttributeOrderStore(private val dbName: String): AttributeOrderStore {

    init {
        Database.connect({ ConnectionProvider.connection(dbName) })
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGAttributeIndexes)
        }
    }

    override fun idToIndex(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        transaction {
            PGAttributeIndex.all().forEach{
                result[it.id.value] = it.attributeIndex
            }
        }
        return result
    }

    override fun store(id: Int, index: Int) {
        transaction {
            PGAttributeIndex.new(id) {
                attributeIndex = index
            }
        }
    }

    override fun load(idToIndex: Map<Int, Int>) {
        require(idToIndex().isEmpty()) {
            "Cannot load attribute order store if it is non-empty."
        }
        transaction {
            idToIndex.forEach {
                PGAttributeIndex.new(it.key) {
                    attributeIndex = it.value
                }
            }
        }
    }
}
object PGAttributeIndexes: IntIdTable(name = ATTRIBUTE_INDEXES_TABLE) {
    val attributeIndex = integer("attr_index")
}
class PGAttributeIndex(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGAttributeIndex>(PGAttributeIndexes)
    var attributeIndex by PGAttributeIndexes.attributeIndex
}