package io.rippledown.model.rule

import io.rippledown.model.RDRCase
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import kotlin.random.Random

class RuleBuildingSession(
    private val ruleFactory: RuleFactory,
    private val tree: RuleTree,
    val case: RDRCase,
    val action: RuleTreeChange,
    cornerstones: List<RDRCase>,
    private val resolver: DefinitionResolver = NO_DEFINITIONS
) {
    var conditions = mutableSetOf<Condition>()
    private val cornerstonesNotExempted = mutableSetOf<RDRCase>()

    /**
     * The session case and cornerstones with their derived values written
     * by the current tree, so that conditions on derived attributes can be
     * evaluated during rule building.
     */
    val materialisedCase: RDRCase
    private val materialisedCornerstones = mutableMapOf<RDRCase, RDRCase>()

    class TemporaryRuleFactory : RuleFactory {
        override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>) =
            Rule(Random.nextInt(), parent, conditions, mutableSetOf(), assignment)
    }

    init {
        // Get a copy of the rule tree.
        val copyOfTree = tree.copy()
        // Make the change to the copied tree.
        copyOfTree.apply(case, resolver)
        action.createChanger(copyOfTree, TemporaryRuleFactory()).updateRuleTree(case, emptySet())

        // Interpret each cornerstone against the modified tree
        // and also the original. Those cases for which these interpretations
        // differ are conflicting cornerstones.
        cornerstones
            .filter { case.name != it.name }
            .forEach {
                copyOfTree.apply(it, resolver)
                val assignmentsGivenByModifiedTree = it.interpretation.assignments()
                val materialised = tree.materialise(it, resolver)
                val assignmentsGivenByOriginalTree = it.interpretation.assignments()
                if (assignmentsGivenByModifiedTree != assignmentsGivenByOriginalTree) {
                    cornerstonesNotExempted.add(it)
                    materialisedCornerstones[it] = materialised
                }
            }
        materialisedCase = tree.materialise(case, resolver)
    }

    fun cornerstoneCases(): List<RDRCase> {
        return cornerstonesNotExempted
            .filter(this::caseSatisfiesConditions)
            .sortedBy { it.name } //we want a predictable order
    }

    fun exemptCornerstone(cornerstone: RDRCase): RuleBuildingSession {
        cornerstonesNotExempted.remove(cornerstone)
        return this
    }

    private fun caseSatisfiesConditions(case: RDRCase): Boolean {
        val materialised = materialisedCornerstones[case] ?: case
        return conditions.all { it.holds(materialised) }
    }

    fun addCondition(condition: Condition): RuleBuildingSession {
        require(condition.holds(materialisedCase)) {
            "Condition $condition was not true for the case ${case.name}"
        }
        conditions.add(condition)
        return this
    }

    fun removeCondition(condition: Condition): RuleBuildingSession {
        conditions.remove(condition)
        return this
    }

    fun commit(): Set<Rule> {
        return action.createChanger(tree, ruleFactory).updateRuleTree(case, conditions)
    }
}