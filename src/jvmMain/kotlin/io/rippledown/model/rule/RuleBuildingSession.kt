package io.rippledown.model.rule

import io.rippledown.kb.RuleManager
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import kotlin.random.Random

class RuleBuildingSession(
    private val ruleManager: RuleManager,
    private val tree: RuleTree,
    val case: RDRCase,
    private val action: RuleTreeChange,
    cornerstones: Set<RDRCase>) {
    var conditions = mutableSetOf<Condition>()
    private val cornerstonesNotExempted = mutableSetOf<RDRCase>()

    class TemporaryRuleFactory: RuleFactory {
        override fun create(parent: Rule, conclusion: Conclusion?, conditions: Set<Condition>) = Rule(Random.nextInt(), parent, conclusion, conditions)
    }

    init {
        // Get a copy of the rule tree.
        val copyOfTree = tree.copy()
        // Make the change to the copied tree.
        copyOfTree.apply(case)
        action.createChanger(copyOfTree, TemporaryRuleFactory()).updateRuleTree(case, emptySet())

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
        return action.createChanger(tree, ruleManager).updateRuleTree(case, conditions)
    }
}