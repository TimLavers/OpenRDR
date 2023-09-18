package io.rippledown.persistence.postgres

import io.rippledown.persistence.OrderStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val CONCLUSION_INDEXES_TABLE = "conclusion_indexes"

class PostgresConclusionOrderStore(private val db: Database) : OrderStore {

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGConclusionIndexes)
        }
    }

    override fun idToIndex(): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        transaction(db) {
            PGConclusionIndex.all().forEach {
                result[it.id.value] = it.conclusionIndex
            }
        }
        return result
    }

    override fun store(id: Int, index: Int) {
        transaction(db) {
            PGConclusionIndex.new(id) {
                conclusionIndex = index
            }
        }
    }

    override fun load(idToIndex: Map<Int, Int>) {
        require(idToIndex().isEmpty()) {
            "Cannot load conclusion order store if it is non-empty."
        }
        transaction(db) {
            idToIndex.forEach {
                PGConclusionIndex.new(it.key) {
                    conclusionIndex = it.value
                }
            }
        }
    }

    object PGConclusionIndexes : IntIdTable(name = CONCLUSION_INDEXES_TABLE) {
        val conclusionIndex = integer("conc_index")
    }

    class PGConclusionIndex(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<PGConclusionIndex>(PGConclusionIndexes)

        var conclusionIndex by PGConclusionIndexes.conclusionIndex
    }
}