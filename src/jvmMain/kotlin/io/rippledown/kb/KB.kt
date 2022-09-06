package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.rule.*

class KB {
    val cornerstones = mutableSetOf<RDRCase>()

    val ruleTree = RuleTree()

    fun startSession(case: RDRCase, action: RuleTreeChange): RuleBuildingSession {
        //interpret the case and all cornerstones at the start of each session
        val cornerstoneToInterpretation = mutableMapOf<RDRCase, Interpretation>()
        cornerstones.forEach { cornerstoneToInterpretation[it] = interpret(it) }
        return RuleBuildingSession(case, interpret(case), action, cornerstoneToInterpretation)
    }

    fun interpret(case: RDRCase): Interpretation {
        println("interpreting $case")
        return ruleTree.apply(case)
    }

    fun addCornerstone(case: RDRCase) {
        cornerstones.add(case)
    }
}