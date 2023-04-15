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

class PostgresAttributeStore(private val dbName: String): AttributeStore {

    init {
        Database.connect({ ConnectionProvider.connection(dbName) })
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGAttributes)
        }
    }

    override fun create(name: String): Attribute {
        val isNew = all().count { it.name == name } == 0 // todo use sql
        require (isNew) {
            "An attribute with name $name already exists."
        }
        var pgAttribute: PGAttribute? = null
        transaction {
            pgAttribute = PGAttribute.new {
                attributeName = name
            }
        }
        return Attribute(pgAttribute!!.attributeName, pgAttribute!!.id.value)
    }

    override fun all(): Set<Attribute> {
        val result = mutableSetOf<Attribute>()
        transaction {
            PGAttribute.all().forEach {
                result.add(Attribute(it.attributeName, it.id.value))
            }
        }
        return result
    }

    override fun store(attribute: Attribute) {
        transaction {
            PGAttribute[attribute.id].attributeName = attribute.name
        }
    }

    override fun load(attributes: Set<Attribute>) {
        require(all().isEmpty()) {
            "Cannot load attributes if there are are some stored already."
        }
        transaction {
            attributes.forEach{
                PGAttribute.new(it.id) {
                    attributeName = it.name
                }
            }
        }
    }
}
object PGAttributes: IntIdTable(name = ATTRIBUTES_TABLE) {
    val attributeName = varchar("name", 256)
}
class PGAttribute(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGAttribute>(PGAttributes)
    var attributeName by PGAttributes.attributeName
}