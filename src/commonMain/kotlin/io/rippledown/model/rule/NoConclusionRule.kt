package io.rippledown.model.rule

import io.rippledown.model.condition.Condition

class NoConclusionRule(id: String, conditions: Set<Condition> = mutableSetOf()) : Rule(id,null, null, conditions) {
    override fun toString(): String {
        return "NoConclusionRule(conditions=$conditions)"
    }
}