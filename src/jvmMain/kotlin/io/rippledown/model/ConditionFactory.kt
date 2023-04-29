package io.rippledown.model

import io.rippledown.model.condition.Condition

interface ConditionFactory {
    fun create(condition: Condition): Condition
}