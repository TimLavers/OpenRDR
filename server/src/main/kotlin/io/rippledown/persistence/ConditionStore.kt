package io.rippledown.persistence

import io.rippledown.model.condition.Condition

interface ConditionStore {
    fun all(): Set<Condition>
    fun create(condition: Condition): Condition
    fun update(condition: Condition)
    fun load(conditions: Set<Condition>)
}
