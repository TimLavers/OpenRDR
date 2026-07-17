package io.rippledown.persistence.postgres

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val ATTRIBUTES_TABLE = "attributes"

class PostgresAttributeStore(private val db: Database) : AttributeStore {

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGAttributes)
        }
    }

    override fun create(name: String, kind: AttributeKind): Attribute {
        val isNew = all().count { it.name == name } == 0 // todo use sql
        require(isNew) {
            "An attribute with name $name already exists."
        }
        return transaction(db) {
            val pgAttribute = PGAttribute.new {
                attributeName = name
                attributeKind = kind.name
            }
            return@transaction Attribute(pgAttribute.id.value, pgAttribute.attributeName, kind)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGAttribute.all()
            .map { Attribute(it.id.value, it.attributeName, AttributeKind.valueOf(it.attributeKind)) }.toSet()
    }

    override fun store(attribute: Attribute) = transaction(db) {
        val pgAttribute = PGAttribute[attribute.id]
        pgAttribute.attributeName = attribute.name
        pgAttribute.attributeKind = attribute.kind.name
    }

    override fun load(attributes: Set<Attribute>) {
        require(all().isEmpty()) {
            "Cannot load attributes if there are are some stored already."
        }
        transaction(db) {
            attributes.forEach {
                PGAttribute.new(it.id) {
                    attributeName = it.name
                    attributeKind = it.kind.name
                }
            }
        }
    }
}

object PGAttributes : IntIdTable(name = ATTRIBUTES_TABLE) {
    val attributeName = varchar("name", 256)
    val attributeKind = varchar("kind", 32).default(AttributeKind.EXTERNAL.name)
}

class PGAttribute(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGAttribute>(PGAttributes)

    var attributeName by PGAttributes.attributeName
    var attributeKind by PGAttributes.attributeKind
}