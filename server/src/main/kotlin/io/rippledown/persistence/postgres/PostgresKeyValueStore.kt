package io.rippledown.persistence.postgres

import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val META_DATA_STORE = "meta_data"

class PostgresKeyValueStore(private val db: Database) : KeyValueStore {

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGKeyValues)
        }
    }

    override fun create(key: String, value: String): KeyValue {
        val isNew = all().count { it.key == key } == 0
        require(isNew) {
            "A pair with key $key already exists."
        }
        return transaction(db) {
            val pgKeyValue = PGKeyValue.new {
                this.key = key
                this.value = value
            }
            return@transaction KeyValue(pgKeyValue.id.value, pgKeyValue.key, pgKeyValue.value)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGKeyValue.all().map { KeyValue(it.id.value, it.key, it.value) }.toSet()
    }

    override fun store(keyValue: KeyValue) {
        val existingWithId = all().filter { it.key == keyValue.key }.firstOrNull()
        require(existingWithId != null && existingWithId.key == keyValue.key) {
            "No element with matching id and key could be found."
        }
        transaction(db) {
            PGKeyValue[keyValue.id].key = keyValue.key
            PGKeyValue[keyValue.id].value = keyValue.value
        }
    }

    override fun load(data: Set<KeyValue>) {
        require(all().isEmpty()) {
            "Cannot load key/value items if there are are some stored already."
        }
        transaction(db) {
            data.forEach {
                PGKeyValue.new(it.id) {
                    key = it.key
                    value = it.value
                }
            }
        }
    }
}
object PGKeyValues : IntIdTable(name = META_DATA_STORE) {
    val key = varchar("key", 256)
    val value = varchar("value", 256 * 16)
}

class PGKeyValue(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGKeyValue>(PGKeyValues)

    var key by PGKeyValues.key
    var value by PGKeyValues.value
}