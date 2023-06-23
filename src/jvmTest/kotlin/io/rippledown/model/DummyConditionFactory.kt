package io.rippledown.model

import io.rippledown.model.condition.Condition
import io.rippledown.persistence.inmemory.copyByReflection

class DummyConditionFactory: ConditionFactory {
    private val idToCondition = mutableMapOf<Int, Condition>()

    override fun getOrCreate(condition: Condition): Condition {
        val existing = idToCondition.values.firstOrNull { it.sameAs( condition) }
        if (existing != null) {
            return existing
        }
        val newId = (idToCondition.keys.maxOrNull() ?: 0) + 1
        val newCondition = copyByReflection(condition, mapOf("id" to newId)) as Condition
        idToCondition[newId] = newCondition
        return newCondition
    }
}