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

open class ChangeTreeToRemoveConclusion(private val toBeRemoved: Conclusion, tree: RuleTree) : RuleTreeChange(tree) {
    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val interpretation = tree.apply(case)
        val rulesChanged = mutableSetOf<Rule>()
        interpretation.rulesGivingConclusion(toBeRemoved)
                .forEach {
                    val stoppingRule = tree.rule(null, conditions)
                    it.addChild(stoppingRule)
                    rulesChanged.add(stoppingRule)
                }
        return rulesChanged
    }

    // Doesn't this depend on which rules are giving the conclusion??????
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean = conclusions.contains(toBeRemoved)
}

class ChangeTreeToReplaceConclusion(private val toBeReplaced: Conclusion, private val toBeReplacement: Conclusion, tree: RuleTree) : RuleTreeChange(tree) {

    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val interpretation = tree.apply(case)
        val rulesChanged = mutableSetOf<Rule>()
        interpretation.rulesGivingConclusion(toBeReplaced)
                .forEach {
                    val child = tree.rule(toBeReplacement, conditions)
                    it.addChild(child)
                    rulesChanged.add(child)
                }
        return rulesChanged
    }

    // Doesn't this depend on which rules are giving the conclusion??????
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean {
        return conclusions.contains(toBeReplaced) && !conclusions.contains(toBeReplacement)
    }
}