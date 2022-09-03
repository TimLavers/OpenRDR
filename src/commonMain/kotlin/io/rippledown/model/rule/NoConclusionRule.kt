package io.rippledown.model.rule

import io.rippledown.model.condition.Condition

class NoConclusionRule(conditions: Set<Condition> = mutableSetOf()) : Rule(null, null, conditions) {
    override fun toString(): String {
        return "NoConclusionRule(conditions=$conditions)"
    }
}