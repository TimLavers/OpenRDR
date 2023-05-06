package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.persistence.ConditionStore
import io.rippledown.persistence.RuleStore

class RuleManager(private val attributeManager: AttributeManager,
                  private val conclusionManager: ConclusionManager,
                  private val conditionManager: ConditionManager,
                  private val ruleStore: RuleStore ): RuleFactory {

    fun ruleTree() : RuleTree {
        TODO()
    }

    override fun create(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>): Rule {
        TODO("Not yet implemented")
    }
}