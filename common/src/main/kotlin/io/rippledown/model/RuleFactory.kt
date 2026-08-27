package io.rippledown.model

import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.Rule

interface RuleFactory {
    /**
     * Creates a rule under [parent] that makes [assignment] when its
     * conditions hold, or that makes no assignment — a stopping rule,
     * retracting what its parent assigned — if [assignment] is null.
     */
    fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>): Rule
}