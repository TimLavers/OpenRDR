package io.rippledown.kb

import io.rippledown.log.lazyLogger
import io.rippledown.model.CaseType
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.RuleSessionRecorder
import io.rippledown.persistence.PersistentKB


class KB(persistentKB: PersistentKB) {
    val logger = lazyLogger

    val kbInfo = persistentKB.kbInfo()
    val metaInfo = MetaInfo(persistentKB.metaDataStore())
    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val conditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    val interpretationViewManager = InterpretationViewManager(persistentKB.conclusionOrderStore(), conclusionManager)
    val ruleSessionRecorder = RuleSessionRecorder(persistentKB.ruleSessionRecordStore())
    internal val ruleManager = RuleManager(conclusionManager, conditionManager, persistentKB.ruleStore())
    private val caseManager = CaseManager(persistentKB.caseStore(), attributeManager)
    internal val caseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)
    val ruleTree = ruleManager.ruleTree()

    fun attributeNames() = attributeManager.all().map { it.name }

    fun description() = metaInfo.getDescription()

    fun setDescription(description: String) {
        metaInfo.setDescription(description)
    }

    fun containsCornerstoneCaseWithName(caseName: String): Boolean {
        return caseManager.ids(CaseType.Cornerstone).find { rdrCase -> rdrCase.name == caseName } != null
    }

    fun loadCases(data: List<RDRCase>) = caseManager.load(data)

    fun addCornerstoneCase(case: RDRCase): RDRCase {
        return caseManager.add(case.copyWithoutId(CaseType.Cornerstone))
    }

    fun addCornerstoneCase(externalCase: ExternalCase): RDRCase {
        val builder = RDRCaseBuilder().apply { setCaseType(CaseType.Cornerstone) }
        externalCase.data.forEach {
            val attribute = attributeManager.getOrCreate(it.key.testName)
            builder.addResult(attribute, it.key.testTime, it.value)
        }
        return caseManager.add(builder.build(externalCase.name))
    }

    fun addProcessedCase(case: RDRCase): RDRCase {
        return caseManager.add(case)
    }

    fun getCaseByName(caseName: String): RDRCase {
        return caseManager.all().first { caseName == it.name }
    }

    fun getCornerstoneCaseByName(caseName: String) = allCornerstoneCases().first { caseName == it.name } // todo test
    fun getProcessedCaseByName(caseName: String) = allProcessedCases().first { caseName == it.name } // todo test

    fun allCornerstoneCases() = caseManager.all(CaseType.Cornerstone)

    fun cornerstoneCaseIds() = caseManager.ids(CaseType.Cornerstone)

    fun processedCaseIds() = caseManager.ids(CaseType.Processed)

    fun allProcessedCases() = caseManager.all(CaseType.Processed)

    fun deletedProcessedCaseWithName(name: String) {
        val toGo = processedCaseIds().firstOrNull { it.name == name }
        if (toGo != null) {
            caseManager.delete(toGo.id!!)
        }
    }

    fun getProcessedCase(id: Long): RDRCase? = caseManager.getCase(id)

    fun getCase(id: Long): RDRCase? = caseManager.getCase(id) // todo test

    fun processCase(externalCase: ExternalCase): RDRCase {
        val case = createRDRCase(externalCase)
        val stored = caseManager.add(case)
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

    fun startRuleSession(
        case: RDRCase,
        action: RuleTreeChange
    ): CornerstoneStatus {
        logger.info("KB starting rule session for case ${case.name} and action $action")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(conclusionManager)
        ruleSession = RuleBuildingSession(ruleManager, ruleTree, case, alignedAction, allCornerstoneCases())
        logger.info("KB rule session created")
        return cornerstoneStatus(null)
    }

    override fun startRuleSessionToAddComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        currentDiff = Addition(comment)
        val conclusion = conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToAddConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        currentDiff = Removal(comment)
        val conclusion = conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToRemoveConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus {
        currentDiff = Replacement(replacedComment, replacementComment)
        val replacedConclusion = conclusionManager.getOrCreate(replacedComment)
        val replacementConclusion = conclusionManager.getOrCreate(replacementComment)
        val action = ChangeTreeToReplaceConclusion(replacedConclusion, replacementConclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun sendCornerstoneStatus() {
        val cornerstoneStatus = cornerstoneStatus(selectedCornerstone)
        runBlocking { webSocketManager?.sendStatus(cornerstoneStatus) }
    }

    override fun sendRuleSessionCompleted() {
        runBlocking { webSocketManager?.sendRuleSessionCompleted() }
    }

    override fun removeCondition(conditionId: Int): CornerstoneStatus {
        check(ruleSession != null) { "No rule session in progress." }
        val condition = conditionManager.getById(conditionId)
        ruleSession!!.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    override fun removeConditionByText(conditionText: String): CornerstoneStatus {
        check(ruleSession != null) { "No rule session in progress." }
        val condition = ruleSession!!.conditions.firstOrNull { it.asText() == conditionText }
            ?: throw IllegalArgumentException("Condition not found in current rule session: $conditionText")
        ruleSession!!.removeCondition(condition)
        return cornerstoneStatus(null)
    }

    fun cancelRuleSession() {
        check(ruleSession != null) { "No rule session in progress." }
        ruleSession = null
        currentDiff = null
    }

    override fun cancelCurrentRuleSession() = cancelRuleSession()

    fun conflictingCasesInCurrentRuleSession(): List<RDRCase> {
        checkSession()
        return ruleSession!!.cornerstoneCases()
    }

    override fun addConditionToCurrentRuleSession(condition: Condition) {
        checkSession()
        // Align the provided condition with that in the condition manager.
        val conditionToUse = if (condition.id == null) {
            conditionManager.getOrCreate(condition)
        } else {
            val existing = conditionManager.getById(condition.id!!)
            // Check that there's no confusion between the condition provided
            // and the one that already exists (here we're defending against test code
            // that might have mixed things up).
            require(existing.sameAs(condition)) {
                "Condition provided does not match that in the condition manager."
            }
            existing
        }
        ruleSession!!.addCondition(conditionToUse)
    }

    override fun commitCurrentRuleSession() {
        checkSession()
        val rulesAdded = ruleSession!!.commit()
        ruleSessionRecorder.recordRuleSessionCommitted(rulesAdded)
        addCornerstoneCase(ruleSession!!.case)
        ruleSession = null
        currentDiff = null
        checkRuleSessionHistoryConsistency()
    }

    override fun exemptCornerstoneCase() = exemptCornerstone(cornerstoneStatus().indexOfCornerstoneToReview)

    override fun selectCornerstoneCase(index: Int) = selectCornerstone(index)

    fun descriptionOfMostRecentRule(): UndoRuleDescription {
        val record = ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()
            ?: return UndoRuleDescription("There are no rules to undo.", false)
        val idOfExemplar = record.idsOfRulesAddedInSession.random()
        val exemplar = ruleTree.ruleForId(idOfExemplar)
        return UndoRuleDescription(exemplar.actionSummary(), true)
    }

    fun ruleSessionHistories() = ruleSessionRecorder.allRuleSessionHistories()

    override fun undoLastRuleSession() {
        val record = ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()!!
        record.idsOfRulesAddedInSession.forEach{
            val toDelete = ruleTree.ruleForId(it)
            ruleManager.deleteLeafRule(toDelete)
        }
        ruleSessionRecorder.delete(ruleSessionRecorder.allRuleSessionHistories().last())
    }

    private fun checkSession() {
        logger.debug { "checking session in KB ${this.kbInfo}" }
        check(ruleSession != null) { "Rule session not started." }
    }

    fun interpret(case: RDRCase) = ruleTree.apply(case)

    fun viewableCase(case: RDRCase): ViewableCase {
        val interpretation = interpret(case)
        val viewableInterpretation = interpretationViewManager.viewableInterpretation(interpretation)
        return caseViewManager.getViewableCase(case, viewableInterpretation)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KB

        return kbInfo == other.kbInfo
    }

    override fun hashCode() = kbInfo.hashCode()
}

internal fun String.normalizeForComparison() =
    lowercase().replace("\"", "").replace("'", "").replace(Regex("\\s+"), " ").trim()