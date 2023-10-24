package io.rippledown.persistence.postgres

import io.rippledown.persistence.VerifiedTextStore
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Persists the mapping from case id to verified text.
 */
class PostgresVerifiedTextStore(private val db: Database) : VerifiedTextStore {
    companion object {
        const val TABLE_NAME = "verified_text"
    }

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGStringTable)
        }
    }

    override fun get(id: Long) = transaction(db) {
        return@transaction PGStringTable
            .select { PGStringTable.id eq id.toInt() }
            .map<ResultRow, String> { it[PGStringTable.text] }
            .firstOrNull<String>()
    }

    override fun put(id: Long, text: String) {
        transaction(db) {
            PGStringTable.insert {
                it[this.id] = id.toInt()
                it[this.text] = text
            }
        }
    }

    object PGStringTable : IntIdTable(name = TABLE_NAME) {
        val text = varchar("text", 10_000)
    }
}