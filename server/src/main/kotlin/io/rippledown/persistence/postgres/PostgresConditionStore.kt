package io.rippledown.persistence.postgres

import io.rippledown.model.condition.Condition
import io.rippledown.persistence.ConditionStore
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

const val CONDITIONS_TABLE = "conditions"

class PostgresConditionStore(private val db: Database): ConditionStore {

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGConditions)
        }
    }

    override fun all() = transaction(db) {
        val result = mutableSetOf<Condition>()
        PGCondition.all().forEach {
            result.add(convertJSONToConditionAndInsertId(it.conditionJSON, it.id.value))
        }
        return@transaction result
    }

    override fun create(condition: Condition): Condition {
        val json = Json.encodeToString(condition)
        return transaction(db) {
            val pgCondition = PGCondition.new {
                conditionJSON = json
            }
            return@transaction convertJSONToConditionAndInsertId(pgCondition.conditionJSON, pgCondition.id.value)
        }
    }

    override fun load(conditions: Set<Condition>) {
        require(all().isEmpty()) {
            "Cannot load conditions if there are are some stored already."
        }
        transaction(db) {
            conditions.forEach {
                PGCondition.new(it.id) {
                    conditionJSON = Json.encodeToString(it)
                }
            }
        }
    }

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
object PGConditions: IntIdTable(name = CONDITIONS_TABLE) {
    val conditionJSON = varchar("json", 2048)
}
class PGCondition(id: EntityID<Int>): IntEntity(id){
    companion object: IntEntityClass<PGCondition>(PGConditions)
    var conditionJSON by PGConditions.conditionJSON
}