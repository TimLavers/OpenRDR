package io.rippledown.model.rule

import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition

class RuleBuildingSession(val case: RDRCase,
                          val action: RuleTreeChange,
                          val cornerstones: Set<RDRCase>) {
    var conditions = mutableSetOf<Condition>()
    private val cornerstonesNotExempted = cornerstones.toMutableSet()

    fun cornerstoneCases(): Set<RDRCase> {
        return cornerstonesNotExempted
                .filter { case.name != it.name }
                .filter(this::caseSatisfiesConditions)
                .filter(this::wouldChangeInterpretation)
                .toSet()
    }

    fun exemptCornerstone(cornerstone: RDRCase): RuleBuildingSession {
        cornerstonesNotExempted.remove(cornerstone)
        return this
    }

    private fun caseSatisfiesConditions(case: RDRCase): Boolean {
        return conditions.all { it.holds(case) }
    }

    private fun wouldChangeInterpretation(case: RDRCase): Boolean {
        val interpForCase = case.interpretation
        return action.wouldChangeConclusions(interpForCase.conclusions())
    }

    fun addCondition(condition: Condition): RuleBuildingSession {
        if (condition.holds(case))
            conditions.add(condition)
        else throw Exception("Condition $condition was not true for the case")
        return this
    }

    fun removeCondition(condition: Condition): RuleBuildingSession {
        conditions.remove(condition)
        return this
    }

    fun commit(): Set<Rule> {
        return action.updateRuleTree(case, conditions)
    }
}