package io.rippledown.model.rule

import io.rippledown.model.RDRCase
import io.rippledown.model.Interpretation
import io.rippledown.model.RuleFactory
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.evaluation.EvaluationContext
import kotlin.random.Random

class RuleBuildingSession(
    private val ruleFactory: RuleFactory,
    private val tree: RuleTree,
    case: RDRCase,
    val action: RuleTreeChange,
    cornerstones: List<RDRCase>,
    private val resolver: DefinitionResolver = NO_DEFINITIONS,
    private val context: EvaluationContext = EvaluationContext.EMPTY,
    private val originalResolver: DefinitionResolver = resolver
) {
    val case = case.isolated()
    var conditions = mutableSetOf<Condition>()
    private val cornerstonesNotExempted = mutableSetOf<RDRCase>()

    /**
     * The cornerstones found conflicting when the session started, whether or
     * not they were later exempted or excluded by a condition. Anything outside
     * this set was never before the user. Ids, not names: two stored cornerstones
     * can share a name.
     */
    val idsOfConflictingCornerstonesAtStart: Set<Long?>

    /**
     * The session case and cornerstones with their derived values written
     * by the current tree, so that conditions on derived attributes can be
     * evaluated during rule building.
     */
    val materialisedCase: RDRCase
    val previewCase: RDRCase
    private val materialisedCornerstones = mutableMapOf<RDRCase, RDRCase>()

    class TemporaryRuleFactory : RuleFactory {
        override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>) =
            Rule(Random.nextInt(), parent, conditions, mutableSetOf(), assignment)
    }

    init {
        // Get a copy of the rule tree.
        val copyOfTree = tree.copy()
        // Make the change to the copied tree.
        copyOfTree.apply(this.case, originalResolver, context)
        action.createChanger(copyOfTree, TemporaryRuleFactory(), originalResolver, context).updateRuleTree(this.case, emptySet())
        previewCase = copyOfTree.materialise(this.case.isolated(), resolver, context)

        // Interpret each cornerstone against the modified tree
        // and also the original. Those cases for which these interpretations
        // differ are conflicting cornerstones.
        cornerstones
            .filter { case.name != it.name }
            .forEach {
                val candidate = copyOfTree.materialise(it.isolated(), resolver, context)
                val assignmentsGivenByModifiedTree = candidate.interpretation.assignments()
                val materialised = tree.materialise(it.isolated(), originalResolver, context)
                val assignmentsGivenByOriginalTree = materialised.interpretation.assignments()
                if (assignmentsGivenByModifiedTree != assignmentsGivenByOriginalTree) {
                    cornerstonesNotExempted.add(it)
                    materialisedCornerstones[it] = materialised
                }
            }
        materialisedCase = tree.materialise(this.case, originalResolver, context)
        idsOfConflictingCornerstonesAtStart = cornerstonesNotExempted.map { it.id }.toSet()
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

    fun commit(action: RuleTreeChange = this.action): Set<Rule> {
        return action.createChanger(tree, ruleFactory, originalResolver, context).updateRuleTree(case, conditions)
    }

    private fun RDRCase.isolated() = copy(interpretation = Interpretation(caseId))
}
