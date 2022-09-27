package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

abstract class RuleTreeChange {
    abstract fun updateRuleTree(tree: RuleTree, case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>
}

class ChangeTreeToAddConclusion(private val toBeAdded: Conclusion) : RuleTreeChange() {
    override fun updateRuleTree(tree: RuleTree, case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = tree.rule(toBeAdded, conditions)
        tree.root.addChild(rule)
        return setOf(rule)
    }
}

open class ChangeTreeToRemoveConclusion(val toBeRemoved: Conclusion) : RuleTreeChange() {
    override fun updateRuleTree(tree: RuleTree, case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        tree.apply(case)
        val interpretation = case.interpretation
        val rulesChanged = mutableSetOf<Rule>()
        val ruleIds = interpretation.idsOfRulesGivingConclusion(toBeRemoved)
        val rulesGivingConclusion = tree.rules().filter { rule -> ruleIds.contains(rule.id)  }
        rulesGivingConclusion.forEach {
                    val newChild = createRule(tree, conditions)
                    it.addChild(newChild)
                    rulesChanged.add(newChild)
                }
        return rulesChanged
    }

    open fun createRule(tree: RuleTree, conditions: Set<Condition>): Rule {
        return tree.rule(null, conditions)
    }
}

class ChangeTreeToReplaceConclusion(toBeReplaced: Conclusion, private val replacement: Conclusion) : ChangeTreeToRemoveConclusion(toBeReplaced) {
    override fun createRule(tree: RuleTree, conditions: Set<Condition>): Rule {
        return tree.rule(replacement, conditions)
    }
}