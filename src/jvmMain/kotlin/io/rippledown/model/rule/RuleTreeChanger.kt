package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition

abstract class RuleTreeChanger(val ruleTree: RuleTree, val ruleFactory: RuleFactory, val conclusionToAdd: Conclusion?) {
    abstract fun isApplicable(case: RDRCase): Boolean

    abstract fun updateRuleTree(case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>

    fun createRule(parent: Rule, conclusionToAdd: Conclusion?, conditions: Set<Condition>): Rule {
        val newRule = ruleFactory.createRuleAndAddToParent(parent, conclusionToAdd, conditions)
        parent.addChild(newRule)
        return newRule
    }
}

class AddConclusionRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, conclusionToAdd: Conclusion?) : RuleTreeChanger(ruleTree, ruleFactory, conclusionToAdd) {
    override fun isApplicable(case: RDRCase): Boolean {
        return !ruleTree.apply(case).conclusions().contains(conclusionToAdd)
    }

    override fun updateRuleTree( case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = createRule(ruleTree.root, conclusionToAdd, conditions)
        return setOf(rule)
    }

    override fun toString(): String {
        return "AddConclusionRuleTreeChanger(toBeAdded=$conclusionToAdd)"
    }
}

open class ModifyConclusionRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, internal val toBeRemoved: Conclusion, conclusionToAdd: Conclusion?) : RuleTreeChanger(ruleTree, ruleFactory, conclusionToAdd) {
    override fun isApplicable(case: RDRCase): Boolean {
        return ruleTree.apply(case).conclusions().contains(toBeRemoved)
    }

    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        ruleTree.apply(case)
        val interpretation = case.interpretation
        val rulesChanged = mutableSetOf<Rule>()
        val ruleIds = interpretation.idsOfRulesGivingConclusion(toBeRemoved)
        val rulesGivingConclusion = ruleTree.rules().filter { rule -> ruleIds.contains(rule.id)  }
        rulesGivingConclusion.forEach {
                    val newChild = createRule(it, conclusionToAdd, conditions)
                    rulesChanged.add(newChild)
                }
        return rulesChanged
    }
}
open class RemoveConclusionRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, toBeRemoved: Conclusion) : ModifyConclusionRuleTreeChanger(ruleTree, ruleFactory, toBeRemoved, null) {
    override fun toString(): String {
        return "RemoveConclusionRuleTreeChanger(toBeRemoved=$toBeRemoved)"
    }
}

class ReplaceConclusionRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, toBeReplaced: Conclusion, replacement: Conclusion) : ModifyConclusionRuleTreeChanger(ruleTree, ruleFactory, toBeReplaced, replacement) {
    override fun toString(): String {
        return "ReplaceConclusionRuleTreeChanger(toBeReplaced=$toBeRemoved replacement=$conclusionToAdd"
    }
}