package io.rippledown.persistence

import io.rippledown.model.Conclusion
import io.rippledown.model.condition.Condition
import io.rippledown.util.copyByReflection

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