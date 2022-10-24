package io.rippledown.model.rule

import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

class RuleBuildingSession(
    private val tree: RuleTree,
    val case: RDRCase,
    private val action: RuleTreeChange,
    cornerstones: Set<RDRCase>) {
    var conditions = mutableSetOf<Condition>()
    private val cornerstonesNotExempted = mutableSetOf<RDRCase>()

    init {
        // Get a copy of the rule tree.
        val copyOfTree = tree.copy()
        // Make the change to the copied tree.
        copyOfTree.apply(case)
        action.updateRuleTree(copyOfTree, case, emptySet())

        // Interpret each cornerstone against the modified tree
        // and also the original. Those cases for which these interpretations
        // differ are conflicting cornerstones.
        cornerstones
            .filter { case.name != it.name }
            .forEach {
            copyOfTree.apply(it)
            val conclusionsGivenByModifiedTree =  it.interpretation.conclusions()
            tree.apply(it)
            val conclusionsGivenByOriginalTree = it.interpretation.conclusions()
            if (conclusionsGivenByModifiedTree != conclusionsGivenByOriginalTree) {
                cornerstonesNotExempted.add(it)
            }
        }
    }

    fun cornerstoneCases(): Set<RDRCase> {
        return cornerstonesNotExempted
                .filter(this::caseSatisfiesConditions)
                .toSet()
    }

    fun exemptCornerstone(cornerstone: RDRCase): RuleBuildingSession {
        cornerstonesNotExempted.remove(cornerstone)
        return this
    }

    private fun caseSatisfiesConditions(case: RDRCase): Boolean {
        return conditions.all { it.holds(case) }
    }

    fun addCondition(condition: Condition): RuleBuildingSession {
        if (condition.holds(case))
            conditions.add(condition)
        else throw Exception("Condition $condition was not true for the case ${case.name}")
        return this
    }

    fun removeCondition(condition: Condition): RuleBuildingSession {
        conditions.remove(condition)
        return this
    }

    fun commit(): Set<Rule> {
        return action.updateRuleTree(tree, case, conditions)
    }
}