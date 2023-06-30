package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.external.ExternalCase
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
    private val cornerstones = CaseManager()
    private val processedCases = CaseManager()
    private var ruleSession: RuleBuildingSession? = null
    val caseViewManager: CaseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)

    fun containsCornerstoneCaseWithName(caseName: String) = !cornerstones.containsCaseWithName(caseName)

    fun loadCornerstoneCases(data: List<RDRCase>) = cornerstones.load(data)
    fun loadProcessedCases(data: List<RDRCase>) = processedCases.load(data)

    fun addCornerstoneCase(case: RDRCase) = cornerstones.add(case)
    fun addProcessedCase(case: RDRCase): RDRCase = processedCases.add(case)

    fun getCornerstoneCaseByName(caseName: String) = cornerstones.firstCaseWithName(caseName)!!
    fun getProcessedCaseByName(caseName: String) = processedCases.firstCaseWithName(caseName)!!

    fun allCornerstoneCases() = cornerstones.all()
    fun allProcessedCases() = processedCases.all()

    fun processedCaseIds() = processedCases.ids()

    fun getProcessedCase(id: Long): RDRCase? = processedCases.getCase(id)
    fun getCornerstoneCase(id: Long): RDRCase? = cornerstones.getCase(id) // todo test

    fun deletedProcessedCaseWithName(name: String) {
        val toGo = processedCases.all().firstOrNull { it.name == name }
        if (toGo != null) {
            processedCases.delete(toGo.id!!)
        }
    }

    fun processCase(externalCase: ExternalCase): RDRCase {
        val case = createRDRCase(externalCase)
        val stored = processedCases.add(case)
        interpret(stored)
        return stored
    }

    fun createRDRCase(case: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder()
        case.data.forEach {
            val attribute = attributeManager.getOrCreate(it.key.testName)
            builder.addResult(attribute, it.key.testTime, it.value)
        }
        return builder.build(case.name)
    }

    fun startRuleSession(case: RDRCase, action: RuleTreeChange) {
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(conclusionManager)
        val sessionCase = case.copyWithoutId()
        ruleSession = RuleBuildingSession(ruleManager, ruleTree, sessionCase, alignedAction, cornerstones.all())
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
        ruleSession!!.commit()
        cornerstones.add(ruleSession!!.case)
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

    override fun hashCode() = kbInfo.hashCode()

    fun conditionHintsForCase(case: RDRCase) = conditionManager.conditionHintsForCase(case)
}