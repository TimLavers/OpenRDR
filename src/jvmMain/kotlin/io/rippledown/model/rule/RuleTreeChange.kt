package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

abstract class RuleTreeChange {
    abstract fun isApplicable(tree: RuleTree, case: RDRCase): Boolean
    abstract fun updateRuleTree(tree: RuleTree, case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>
}

class ChangeTreeToAddConclusion(private val toBeAdded: Conclusion) : RuleTreeChange() {
    override fun isApplicable(tree: RuleTree, case: RDRCase): Boolean {
        return !tree.apply(case).conclusions().contains(toBeAdded)
    }

    override fun updateRuleTree(tree: RuleTree, case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = tree.rule(toBeAdded, conditions)
        tree.root.addChild(rule)
        return setOf(rule)
    }

    override fun toString(): String {
        return "ChangeTreeToAddConclusion(toBeAdded=$toBeAdded)"
    }
}

open class ChangeTreeToRemoveConclusion(internal val toBeRemoved: Conclusion) : RuleTreeChange() {

    override fun isApplicable(tree: RuleTree, case: RDRCase): Boolean {
        return tree.apply(case).conclusions().contains(toBeRemoved)
    }

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

    override fun toString(): String {
        return "ChangeTreeToRemoveConclusion(toBeRemoved=$toBeRemoved)"
    }
}

class ChangeTreeToReplaceConclusion(toBeReplaced: Conclusion, private val replacement: Conclusion) : ChangeTreeToRemoveConclusion(toBeReplaced) {
    override fun createRule(tree: RuleTree, conditions: Set<Condition>): Rule {
        return tree.rule(replacement, conditions)
    }

    override fun toString(): String {
        return "ChangeTreeToReplaceConclusion(toBeReplaced=$toBeRemoved replacement=$replacement)"
    }


}