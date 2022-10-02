package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.*

class KB(val name: String) {
    val cornerstones = mutableSetOf<RDRCase>()
    val ruleTree = RuleTree()
    private var ruleSession: RuleBuildingSession? = null

    fun startSession(case: RDRCase, action: RuleTreeChange): RuleBuildingSession {
        //interpret the case and all cornerstones at the start of each session
//        cornerstones.forEach { interpret(it) }
//        interpret(case)
        return RuleBuildingSession(ruleTree, case, action, cornerstones)
    }

    fun startRuleSessionToAddConclusion(case: RDRCase, conclusion: Conclusion) {
        check(ruleSession == null)
        ruleSession =  startSession(case, ChangeTreeToAddConclusion(conclusion))
    }

    fun startRuleSessionToReplaceConclusion(case: RDRCase, toGo: Conclusion, replacement: Conclusion) {
        check(ruleSession == null)
        ruleSession =  startSession(case, ChangeTreeToReplaceConclusion(toGo, replacement))
    }

    fun addConditionToCurrentRuleSession(condition: Condition){
        check(ruleSession != null)
        ruleSession!!.addCondition(condition)
    }

    fun finishCurrentRuleSession() {
        check(ruleSession != null)
        ruleSession!!.commit()
        ruleSession = null
    }

    fun interpret(case: RDRCase) {
        ruleTree.apply(case)
    }

    fun addCornerstone(case: RDRCase) {
        cornerstones.add(case)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KB

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}