package io.rippledown.persistence.postgres

import io.rippledown.model.condition.Condition
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

const val RULES_TABLE = "rules"

class PostgresRuleStore(private val dbName: String): RuleStore {

    init {
        Database.connect({ ConnectionProvider.connection(dbName) })
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGRules)
        }
    }

    override fun all() = transaction {
        return@transaction PGRule.all().map{ persistentRule(it) }.toSet()
    }

    override fun create(prototype: PersistentRule): PersistentRule {
        return transaction {
            val pgRule = PGRule.new {
                parentId = prototype.parentId
                conclusionId = prototype.conclusionId
                conditionIds = prototype.conditionIdsString()
            }
            return@transaction persistentRule(pgRule)
        }
    }


    override fun load(persistentRules: Set<PersistentRule>) {
        TODO("Not yet implemented")
    }

    private fun persistentRule(pgRule: PGRule) = PersistentRule(pgRule.id.value, pgRule.parentId, pgRule.conclusionId, pgRule.conditionIds)
    //    override fun all()
//
//    override fun create(condition: Condition): Condition {
//        val json = Json.encodeToString(condition)
//        return transaction {
//            val pgCondition = PGCondition.new {
//                conditionJSON = json
//            }
//            return@transaction convertJSONToConditionAndInsertId(pgCondition.conditionJSON, pgCondition.id.value)
//        }
//    }
//
//    override fun load(conditions: Set<Condition>) {
//        require(all().isEmpty()) {
//            "Cannot load conditions if there are are some stored already."
//        }
//        transaction {
//            conditions.forEach {
//                PGCondition.new(it.id) {
//                    conditionJSON = Json.encodeToString(it)
//                }
//            }
//        }
//    }

    private fun convertJSONToConditionAndInsertId(json: String, id: Int): Condition {
        val resuscitated: Condition = Json.decodeFromString(json)
        return createCopyWithGivenId(resuscitated, id)
    }

    private fun createCopyWithGivenId(condition: Condition, id: Int): Condition {
        val instanceKClass = condition::class
        val copyFunction = instanceKClass.functions.single { function -> function.name == "copy" }

        val parameterMap = mutableMapOf<KParameter, Any>()
        parameterMap[copyFunction.instanceParameter!!] = condition
        val idParameter = copyFunction.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .single { it.name == "id" }
        parameterMap[idParameter] = id

        return (copyFunction.callBy(parameterMap) ?: IllegalArgumentException("Could not copy $condition.")) as Condition
    }
}
object PGRules: IntIdTable(name = RULES_TABLE) {
    val parentId = integer("parent")
    val conclusionId = integer("conclusion").nullable()
    val conditionIds = varchar("conditions", 1024)
}
class PGRule(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGRule>(PGRules)
    var parentId by PGRules.parentId
    var conclusionId by PGRules.conclusionId
    var conditionIds by PGRules.conditionIds
}