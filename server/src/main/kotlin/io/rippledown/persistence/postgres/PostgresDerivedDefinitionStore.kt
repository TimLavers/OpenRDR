package io.rippledown.persistence.postgres

import io.rippledown.model.rule.ValueExpression
import io.rippledown.persistence.DerivedDefinitionStore
import io.rippledown.persistence.expressionFromString
import io.rippledown.persistence.expressionToString
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val DERIVED_DEFINITIONS_TABLE = "derived_definitions"

class PostgresDerivedDefinitionStore(private val db: Database) : DerivedDefinitionStore {

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGDerivedDefinitions)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGDerivedDefinition.all().associate {
            it.id.value to expressionFromString(it.expression)
        }
    }

    override fun definitionFor(attributeId: Int) = transaction(db) {
        return@transaction PGDerivedDefinition.findById(attributeId)?.let { expressionFromString(it.expression) }
    }

    override fun store(attributeId: Int, expression: ValueExpression) {
        transaction(db) {
            val existing = PGDerivedDefinition.findById(attributeId)
            if (existing == null) {
                PGDerivedDefinition.new(attributeId) {
                    this.expression = expressionToString(expression)
                }
            } else {
                existing.expression = expressionToString(expression)
            }
        }
    }

    override fun load(definitions: Map<Int, ValueExpression>) {
        require(all().isEmpty()) {
            "Cannot load definitions into a non-empty derived definition store."
        }
        transaction(db) {
            definitions.forEach { (attributeId, expression) ->
                PGDerivedDefinition.new(attributeId) {
                    this.expression = expressionToString(expression)
                }
            }
        }
    }
}

object PGDerivedDefinitions : IntIdTable(name = DERIVED_DEFINITIONS_TABLE, columnName = "attribute_id") {
    val expression = text("expression")
}

class PGDerivedDefinition(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PGDerivedDefinition>(PGDerivedDefinitions)

    var expression by PGDerivedDefinitions.expression
}
