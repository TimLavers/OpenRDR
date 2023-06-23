package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.RuleBuildingSession
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.RuleTreeChange
import io.rippledown.persistence.PersistentKB

class KB(persistentKB: PersistentKB) {

    val kbInfo: KBInfo = persistentKB.kbInfo()
    val attributeManager: AttributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager: ConclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val conditionManager: ConditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    private val ruleManager: RuleManager = RuleManager(conclusionManager, conditionManager, persistentKB.ruleStore())
    val ruleTree: RuleTree = ruleManager.ruleTree()
    private var ruleSession: RuleBuildingSession? = null
    val caseViewManager: CaseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)
    private val cornerstones = mutableSetOf<RDRCase>()
    private val idToCase = mutableMapOf<String, RDRCase>() //"processed" cases


    fun allCornerstoneCases()= cornerstones.toSet()

    fun containsCornerstoneCaseWithName(caseName: String): Boolean {
        return cornerstones.find { rdrCase -> rdrCase.name == caseName } != null
    }

    fun addCornerstoneCase(case: RDRCase) {
        if (!containsCornerstoneCaseWithName(case.name)) cornerstones.add(case)
    }


    fun allCases()= idToCase.values.toSet()

    fun getCaseByName(caseName: String): RDRCase {
        return idToCase.values.first { caseName == it.name }
    }

    fun startRuleSession(case: RDRCase, action: RuleTreeChange) {
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(conclusionManager)
        ruleSession = RuleBuildingSession(ruleManager, ruleTree, case, alignedAction, cornerstones)
    }

    fun conflictingCasesInCurrentRuleSession(): List<RDRCase> {
        checkSession()
        return ruleSession!!.cornerstoneCases()
    }

    fun addConditionToCurrentRuleSession(condition: Condition) {
        checkSession()
        // Align the provided condition with that in the condition manager.
        val conditionToUse = if (condition.id == null) {
            conditionManager.getOrCreate(condition)
        } else {
            val existing = conditionManager.getById(condition.id!!)
            // Check that there's no confusion between the condition provided
            // and the one that already exists (here we're defending against test code
            // that might have mixed things up).
            require(existing!!.sameAs(condition)) {
                "Condition provided does not match that in the condition manager."
            }
            existing
        }
        ruleSession!!.addCondition(conditionToUse)
    }

    fun commitCurrentRuleSession() {
        checkSession()

        //The case for which the user is building a rule becomes a cornerstone.
        addCornerstoneCase(ruleSession!!.case)

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

        return kbInfo == other.kbInfo
    }

    override fun hashCode()=kbInfo.hashCode()

    fun conditionHintsForCase(case: RDRCase) = conditionManager.conditionHintsForCase(case)



    //TODO: refactor the following methods using a proper case repository

    fun putCase(case: RDRCase) {
        idToCase.put(case.id,  case)
    }

    fun containsCaseWithId(id: String) = idToCase.containsKey(id)

    fun caseForId(id: String) = idToCase[id]!!
}