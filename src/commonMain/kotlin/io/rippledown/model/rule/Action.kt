package io.rippledown.model.rule

import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

abstract class Action(val tree: RuleTree) {
    abstract fun updateRuleTree(case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>
    abstract fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean

}

/**
 * Adds a rule to the root rule.
 */
class AddAction(private val toBeAdded: Conclusion, tree: RuleTree) : Action(tree) {

    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = tree.rule(toBeAdded, conditions)
        tree.root.addChild(rule)
        return setOf(rule)
    }

    /**
     * Interpretation would change if it does not contain the conclusion that would be added by the action
     */
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean {
        return !conclusions.contains(toBeAdded)
    }
}

open class RemoveAction(private val toBeRemoved: Conclusion, tree: RuleTree) : Action(tree) {

    /**
     * Adds a stopping rule under each rule giving the conclusion to be removed
     *
     * @return the stopping rules
     */
    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val interp = tree.apply(case)
        val rulesChanged = mutableSetOf<Rule>()
        interp.rulesGivingConclusion(toBeRemoved)
                .forEach {
                    val stoppingRule = tree.rule(null, conditions)
                    it.addChild(stoppingRule)
                    rulesChanged.add(stoppingRule)
                }
        return rulesChanged
    }

    /**
     * Interpretation would change if it contains the conclusion to be removed by the action
     */
    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean = conclusions.contains(toBeRemoved)
}

class ReplaceAction(private val toBeReplaced: Conclusion, private val toBeReplacement: Conclusion, tree: RuleTree) : Action(tree) {

    /**
     * Adds rules corresponding to the replacement conclusion under each rule giving the conclusion to be replaced
     *
     * @return the replacement rules
     */
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

    override fun wouldChangeConclusions(conclusions: Set<Conclusion>): Boolean {
        return conclusions.contains(toBeReplaced) && !conclusions.contains(toBeReplacement)
    }
}