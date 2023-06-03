package io.rippledown.model

import io.rippledown.model.condition.Condition

interface ConditionFactory {
    fun getOrCreate(condition: Condition): Condition
}