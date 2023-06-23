package io.rippledown.persistence.inmemory

import io.rippledown.model.condition.Condition
import io.rippledown.persistence.ConditionStore
import kotlin.reflect.KParameter
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

class InMemoryConditionStore: ConditionStore {
    private val conditionSet = mutableSetOf<Condition>()

    override fun all() = conditionSet

    override fun create(condition: Condition): Condition {
        require(condition.id == null) {
            "Cannot create from a condition with a non-null id."
        }
        val maxById = conditionSet.maxByOrNull { it.id!! }
        val maxId = maxById?.id ?: 0
        val newCondition = copyByReflection(condition, mapOf("id" to maxId + 1)) as Condition
        conditionSet.add(newCondition)
        return newCondition
    }

    override fun load(conditions: Set<Condition>) {
        require(conditionSet.isEmpty()) {
            "Cannot load conditions into a non-empty condition store."
        }
        conditionSet.addAll(conditions)
    }
}

// https://xa1.at/copy-data-class-reflection/
fun copyByReflection(instance: Any, newValues: Map<String, Any?>): Any {
    val instanceKClass = instance::class
    require(instanceKClass.isData) { "instance must be data class" }

    val copyFunction = instanceKClass.functions.single { function -> function.name == "copy" }

    val valueArgs = copyFunction.parameters
        .filter { parameter -> parameter.kind == KParameter.Kind.VALUE }
        .mapNotNull { parameter ->
            newValues[parameter.name]?.let { value -> parameter to value }
        }

    return copyFunction.callBy(
        mapOf(copyFunction.instanceParameter!! to instance) + valueArgs
    ) ?: error("copy didn't return a new instance")
}