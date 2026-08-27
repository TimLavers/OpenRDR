package io.rippledown.model.rule

import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition

abstract class RuleTreeChanger(val ruleTree: RuleTree, val ruleFactory: RuleFactory) {
    abstract fun isApplicable(case: RDRCase): Boolean

    abstract fun updateRuleTree(case: RDRCase, conditions: Set<Condition> = setOf()): Set<Rule>

    /**
     * Creates a rule under [parent] making [assignment], or a stopping rule
     * if [assignment] is null.
     */
    fun createRule(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>): Rule {
        val newRule = ruleFactory.createRuleAndAddToParent(parent, assignment, conditions)
        parent.addChild(newRule)
        return newRule
    }
}

class AddAssignmentRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, val assignmentToAdd: AssignValue) :
    RuleTreeChanger(ruleTree, ruleFactory) {
    override fun isApplicable(case: RDRCase): Boolean {
        return !ruleTree.apply(case).assignments().contains(assignmentToAdd)
    }

    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        val rule = createRule(ruleTree.root, assignmentToAdd, conditions)
        return setOf(rule)
    }

    override fun toString(): String {
        return "AddAssignmentRuleTreeChanger(toBeAdded=$assignmentToAdd)"
    }
}

open class ModifyAssignmentRuleTreeChanger(
    ruleTree: RuleTree,
    ruleFactory: RuleFactory,
    internal val assignmentToBeRemoved: AssignValue,
    internal val assignmentToAdd: AssignValue?
) : RuleTreeChanger(ruleTree, ruleFactory) {
    override fun isApplicable(case: RDRCase): Boolean {
        return ruleTree.apply(case).assignments().contains(assignmentToBeRemoved)
    }

    override fun updateRuleTree(case: RDRCase, conditions: Set<Condition>): Set<Rule> {
        ruleTree.apply(case)
        val interpretation = case.interpretation
        val rulesChanged = mutableSetOf<Rule>()
        val ruleIds = interpretation.idsOfRulesMakingAssignment(assignmentToBeRemoved)
        val rulesMakingAssignment = ruleTree.rules().filter { rule -> ruleIds.contains(rule.id) }
        rulesMakingAssignment.forEach {
            rulesChanged.add(createRule(it, assignmentToAdd, conditions))
        }
        return rulesChanged
    }
}

open class RemoveAssignmentRuleTreeChanger(ruleTree: RuleTree, ruleFactory: RuleFactory, toBeRemoved: AssignValue) :
    ModifyAssignmentRuleTreeChanger(ruleTree, ruleFactory, toBeRemoved, null) {
    override fun toString(): String {
        return "RemoveAssignmentRuleTreeChanger(toBeRemoved=$assignmentToBeRemoved)"
    }
}

class ReplaceAssignmentRuleTreeChanger(
    ruleTree: RuleTree,
    ruleFactory: RuleFactory,
    toBeReplaced: AssignValue,
    replacement: AssignValue
) : ModifyAssignmentRuleTreeChanger(ruleTree, ruleFactory, toBeReplaced, replacement) {
    override fun toString(): String {
        return "ReplaceAssignmentRuleTreeChanger(toBeReplaced=$assignmentToBeRemoved replacement=$assignmentToAdd)"
    }
}