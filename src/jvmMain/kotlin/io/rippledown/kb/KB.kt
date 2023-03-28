package io.rippledown.kb

import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.*

class KB(val name: KBInfo,
         val attributeManager: AttributeManager = AttributeManager(),
         val ruleTree: RuleTree = RuleTree()) {

    constructor(name: String) : this(KBInfo(name))

    private val cornerstones = mutableSetOf<RDRCase>()
    private var ruleSession: RuleBuildingSession? = null
    val caseViewManager = CaseViewManager()

    fun containsCaseWithName(caseName: String): Boolean {
        return cornerstones.find { rdrCase -> rdrCase.name == caseName } != null
    }

    fun addCase(case: RDRCase) {
        require(!containsCaseWithName(case.name)) { "There is already a case with name ${case.name} in the KB."}
        cornerstones.add(case)
    }

    fun getCaseByName(caseName: String): RDRCase {
        return cornerstones.first { caseName == it.name }
    }

    fun allCases(): Set<RDRCase> {
        return cornerstones.toSet()
    }

    fun startRuleSession(case: RDRCase, action: RuleTreeChange) {
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) {"Action $action is not applicable to case ${case.name}"}
        ruleSession =  RuleBuildingSession(ruleTree, case, action, cornerstones)
    }

    fun conflictingCasesInCurrentRuleSession(): Set<RDRCase> {
        checkSession()
        return ruleSession!!.cornerstoneCases()
    }

    fun addConditionToCurrentRuleSession(condition: Condition){
        checkSession()
        ruleSession!!.addCondition(condition)
    }

    fun commitCurrentRuleSession() {
        checkSession()
        ruleSession!!.commit()
        ruleSession = null
    }

    private fun checkSession() {
        check(ruleSession != null) { "Rule session not started." }
    }

    fun interpret(case: RDRCase) {
        ruleTree.apply(case)
    }

    fun viewableInterpretedCase(case: RDRCase): ViewableCase {
        interpret(case)
        return caseViewManager.getViewableCase(case)
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