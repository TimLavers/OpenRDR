package io.rippledown.persistence.postgres

import io.rippledown.model.Attribute
import io.rippledown.persistence.AttributeStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

const val ATTRIBUTES_TABLE = "attributes"

class PostgresAttributeStore(private val db: Database) : AttributeStore {

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGAttributes)
        }
    }

    override fun create(name: String): Attribute {
        val isNew = all().count { it.name == name } == 0 // todo use sql
        require(isNew) {
            "An attribute with name $name already exists."
        }
        return transaction(db) {
            val pgAttribute = PGAttribute.new {
                attributeName = name
            }
            return@transaction Attribute(pgAttribute.id.value, pgAttribute.attributeName)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGAttribute.all().map { Attribute(it.id.value, it.attributeName) }.toSet()
    }

    override fun store(attribute: Attribute) = transaction(db) {
            PGAttribute[attribute.id].attributeName = attribute.name
        }

    override fun load(attributes: Set<Attribute>) {
        require(all().isEmpty()) {
            "Cannot load attributes if there are are some stored already."
        }
        transaction(db) {
            attributes.forEach {
                PGAttribute.new(it.id) {
                    attributeName = it.name
                }
            }
        }
    }
}

object PGAttributes : IntIdTable(name = ATTRIBUTES_TABLE) {
    val attributeName = varchar("name", 256)
}

class PGAttribute(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGAttribute>(PGAttributes)

    var attributeName by PGAttributes.attributeName
}