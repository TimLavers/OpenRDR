package io.rippledown.persistence.postgres

import io.rippledown.persistence.OrderStore
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class PostgresConclusionOrderStore(private val db: Database) : OrderStore {
    companion object {
        const val TABLE_NAME = "conclusion_indexes"
    }

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
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

    object PGConclusionIndexes : IntIdTable(name = TABLE_NAME) {
        val conclusionIndex = integer("conc_index")
    }

    class PGConclusionIndex(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<PGConclusionIndex>(PGConclusionIndexes)

        var conclusionIndex by PGConclusionIndexes.conclusionIndex
    }
}