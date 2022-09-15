package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

abstract class RuleTreeChange(val tree: RuleTree) {
    abstract fun updateRuleTree(case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>
    abstract fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean
}

class ChangeTreeToAddConclusion(private val toBeAdded: Conclusion, tree: RuleTree) : RuleTreeChange(tree) {
    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = tree.rule(toBeAdded, conditions)
        tree.root.addChild(rule)
        return setOf(rule)
    }

    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean {
        return !conclusions.contains(toBeAdded)
    }
}

open class ChangeTreeToRemoveConclusion(val toBeRemoved: Conclusion, tree: RuleTree) : RuleTreeChange(tree) {
    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val interpretation = tree.apply(case)
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

    // Doesn't this depend on which rules are giving the conclusion??????
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean = conclusions.contains(toBeRemoved)
}

class ChangeTreeToReplaceConclusion(toBeReplaced: Conclusion, private val replacement: Conclusion, tree: RuleTree) : ChangeTreeToRemoveConclusion(toBeReplaced,tree) {
    override fun createRule(tree: RuleTree, conditions: Set<Condition>): Rule {
        return tree.rule(replacement, conditions)
    }

    // Doesn't this depend on which rules are giving the conclusion??????
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean {
        return conclusions.contains(toBeRemoved) && !conclusions.contains(replacement)
    }
}