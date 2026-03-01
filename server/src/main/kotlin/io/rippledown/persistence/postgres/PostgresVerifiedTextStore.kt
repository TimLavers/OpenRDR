package io.rippledown.persistence.postgres

import io.rippledown.persistence.VerifiedTextStore
import org.jetbrains.exposed.v1.core.StdOutSqlLogger
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
            .selectAll().where { PGStringTable.id eq id.toInt() }
            .map { it[PGStringTable.text] }
            .firstOrNull()
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