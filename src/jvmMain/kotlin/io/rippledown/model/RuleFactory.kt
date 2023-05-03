package io.rippledown.model

import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.Rule

interface RuleFactory {
    fun create(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule
}