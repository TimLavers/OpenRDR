package io.rippledown.persistence.postgres

import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

const val RULES_TABLE = "rules"

class PostgresRuleStore(private val db: Database): RuleStore {

    init {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGRules)
        }
    }

    override fun all() = transaction(db) {
        return@transaction PGRule.all().map{ persistentRule(it) }.toSet()
    }

    override fun create(prototype: PersistentRule): PersistentRule {
        return transaction(db) {
            val pgRule = PGRule.new {
                parentId = prototype.parentId
                conclusionId = prototype.conclusionId
                conditionIds = prototype.conditionIdsString()
            }
            return@transaction persistentRule(pgRule)
        }
    }

    override fun load(persistentRules: Set<PersistentRule>) {
        require(all().isEmpty()) {
            "Cannot load persistent rules if there are some stored already."
        }
        transaction(db) {
            persistentRules.forEach {
                PGRule.new(it.id) {
                    parentId = it.parentId
                    conclusionId = it.conclusionId
                    conditionIds = it.conditionIdsString()
                }
            }
        }
    }

    override fun remove(persistentRule: PersistentRule) {
        transaction(db) {
            PGRules.deleteWhere {
                id eq persistentRule.id
            }
        }
    }

    private fun persistentRule(pgRule: PGRule) = PersistentRule(pgRule.id.value, pgRule.parentId, pgRule.conclusionId, pgRule.conditionIds)
}
object PGRules: IntIdTable(name = RULES_TABLE) {
    val parentId = integer("parent").nullable()
    val conclusionId = integer("conclusion").nullable()
    val conditionIds = varchar("conditions", 1024)
}
class PGRule(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGRule>(PGRules)
    var parentId by PGRules.parentId
    var conclusionId by PGRules.conclusionId
    var conditionIds by PGRules.conditionIds
}