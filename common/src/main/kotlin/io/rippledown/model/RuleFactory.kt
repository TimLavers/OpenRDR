package io.rippledown.model

import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule

interface RuleFactory {
    fun createRuleAndAddToParent(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule

    fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue, conditions: Set<Condition>): Rule
}