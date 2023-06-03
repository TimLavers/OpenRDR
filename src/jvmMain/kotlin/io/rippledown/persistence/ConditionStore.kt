package io.rippledown.persistence

import io.rippledown.model.condition.Condition

interface ConditionStore {
    fun all(): Set<Condition>
    fun create(condition: Condition): Condition
    fun load(conditions: Set<Condition>)
}