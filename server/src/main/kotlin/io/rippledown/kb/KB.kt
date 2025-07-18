package io.rippledown.kb

import io.rippledown.chat.Conversation
import io.rippledown.chat.REASON_TRANSFORMER
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.expressionparser.AttributeFor
import io.rippledown.expressionparser.ConditionTip
import io.rippledown.kb.chat.ChatManager
import io.rippledown.kb.chat.KBChatService
import io.rippledown.kb.chat.RuleService
import io.rippledown.log.lazyLogger
import io.rippledown.model.CaseType
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.*
import io.rippledown.persistence.PersistentKB

class KB(persistentKB: PersistentKB) {
    val logger = lazyLogger

    val kbInfo = persistentKB.kbInfo()
    val metaInfo = MetaInfo(persistentKB.metaDataStore())
    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val conditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    private val ruleManager = RuleManager(conclusionManager, conditionManager, persistentKB.ruleStore())
    val ruleTree = ruleManager.ruleTree()
    private val caseManager = CaseManager(persistentKB.caseStore(), attributeManager)
    private var ruleSession: RuleBuildingSession? = null
    val ruleSessionRecorder = RuleSessionRecorder(persistentKB.ruleSessionRecordStore())
    internal val caseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)
    val interpretationViewManager = InterpretationViewManager(persistentKB.conclusionOrderStore(), conclusionManager)

    //a var so it can be mocked in tests
    private var conditionParser: ConditionParser

    val ruleService = object : RuleService {
        override suspend fun buildRuleToAddComment(case: RDRCase, comment: String, conditions: List<Condition>) {
            val conclusion = conclusionManager.getOrCreate(comment)
            val action = ChangeTreeToAddConclusion(conclusion)
            startRuleSession(case, action)
            conditions.forEach { addConditionToCurrentRuleSession(it) }
            commitCurrentRuleSession()
        }

        override suspend fun conditionForExpression(
            case: RDRCase,
            expression: String
        ) = conditionForExpression(expression, case)
    }

    private lateinit var chatManager: ChatManager

    init {
        conditionParser = object : ConditionParser {
            override fun parse(expression: String, attributeFor: AttributeFor) =
                ConditionTip(attributeNames(), attributeFor).conditionFor(expression)
        }
    }

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

    fun addProcessedCase(case: RDRCase): RDRCase {
        return caseManager.add(case)
    }

    fun getCaseByName(caseName: String): RDRCase {
        return caseManager.all().first { caseName == it.name }
    }

    fun getCornerstoneCaseByName(caseName: String) = allCornerstoneCases().first { caseName == it.name } // todo test
    fun getProcessedCaseByName(caseName: String) = allProcessedCases().first { caseName == it.name } // todo test

    fun allCornerstoneCases() = caseManager.all(CaseType.Cornerstone)

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

    fun startRuleSession(case: RDRCase, action: RuleTreeChange) {
        logger.info("KB starting rule session for case ${case.name} and action $action")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(conclusionManager)
        ruleSession = RuleBuildingSession(ruleManager, ruleTree, case, alignedAction, allCornerstoneCases())
        logger.info("KB rule session created")
    }

    fun cancelRuleSession() {
        check(ruleSession != null) { "No rule session in progress." }
        ruleSession = null
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
        val rulesAdded = ruleSession!!.commit()
        ruleSessionRecorder.recordRuleSessionCommitted(rulesAdded)
        addCornerstoneCase(ruleSession!!.case)
        ruleSession = null
        checkRuleSessionHistoryConsistency()
    }

    fun descriptionOfMostRecentRule(): UndoRuleDescription {
        val record = ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()
            ?: return UndoRuleDescription("There are no rules to undo.", false)
        val idOfExemplar = record.idsOfRulesAddedInSession.random()
        val exemplar = ruleTree.ruleForId(idOfExemplar)
        return UndoRuleDescription(exemplar.actionSummary(), true)
    }

    fun ruleSessionHistories() = ruleSessionRecorder.allRuleSessionHistories()

    fun undoLastRuleSession() {
        val record = ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()!!
        record.idsOfRulesAddedInSession.forEach{
            val toDelete = ruleTree.ruleForId(it)
            ruleManager.deleteLeafRule(toDelete)
        }
        ruleSessionRecorder.delete(ruleSessionRecorder.allRuleSessionHistories().last())
    }

    private fun checkSession() {
        logger.info("checking session in KB ${this.kbInfo}")
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

    fun conditionHintsForCase(case: RDRCase): ConditionList {
        val suggester = ConditionSuggester(attributeManager.all(), case)
        return ConditionList(suggester.suggestions())
    }

    /**
     * @param request the request containing the currently selected cornerstone and an updated list of conditions
     *
     * @return the CornerstoneStatus for the current session where the cornerstone specified in the request should remain selected if it is still in the list of cornerstones
     * after the new set of conditions have been applied
     */
    fun updateCornerstone(request: UpdateCornerstoneRequest): CornerstoneStatus {
        checkSession()

        //replace the conditions in the current session with the updated ones
        ruleSession!!.conditions = request.conditionList.conditions.toMutableSet()

        //update the cornerstone status
        val currentCC = request.cornerstoneStatus.cornerstoneToReview
        return cornerstoneStatus(currentCC)
    }

    /**
     * @param index the index of the cornerstone to be exempted
     *
     * @return the CornerstoneStatus for the current session after the specified cornerstone has been exempted
     */
    fun exemptCornerstone(index: Int): CornerstoneStatus {
        checkSession()

        val toExempt = ruleSession!!.cornerstoneCases()[index]
        ruleSession!!.exemptCornerstone(toExempt)

        val cornerstones = ruleSession!!.cornerstoneCases()
        return if (cornerstones.isEmpty()) {
            CornerstoneStatus()
        } else {
            val newCC = cornerstones[index.coerceAtMost(cornerstones.size - 1)]
            cornerstoneStatus(viewableCase(newCC))
        }
    }

    /**
     * @param index the index of the cornerstone to be selected
     * @return the CornerstoneStatus for the current session after the specified cornerstone has been selected
     */
    fun selectCornerstone(index: Int): CornerstoneStatus {
        checkSession()
        val cornerstones = ruleSession!!.cornerstoneCases()
        val caseInstance = cornerstones[index]
        // Because Interpretation is not immutable, we need to copy
        // the case with a new interpretation (copy is not deep)
        // to make this thread safe.
        val newCC = caseInstance.copy(interpretation = Interpretation(caseInstance.caseId))
        return CornerstoneStatus(viewableCase(newCC), index, cornerstones.size)
    }

    /**
     * @return the CornerstoneStatus for the current session where the specified cornerstone should remain selected if it is still in the list of cornerstones
     */
    internal fun cornerstoneStatus(currentCornerstone: ViewableCase?): CornerstoneStatus {
        checkSession()
        val cornerstones: List<RDRCase> = ruleSession!!.cornerstoneCases()
        if (cornerstones.isEmpty()) return CornerstoneStatus()

        //if no cornerstone has been selected yet, or the selected cornerstone is no longer in the list of cornerstones, return the first one
        var index = 0
        if (currentCornerstone != null) {
            index = cornerstones.indexOf(currentCornerstone.case)
        }
        index = if (index >= 0) index else 0
        val cornerstone = cornerstones[index]
        val viewableCornerstone = viewableCase(cornerstone)
        return CornerstoneStatus(viewableCornerstone, index, cornerstones.size)
    }

    //Allow a mock parser to be set so we can avoid connecting to Gemini for all the tests
    fun setConditionParser(parser: ConditionParser) {
        conditionParser = parser
    }

    fun conditionForExpression(expression: String, case: RDRCase): ConditionParsingResult {
        val attributeFor: AttributeFor = { attributeManager.getOrCreate(it) }
        val condition = conditionParser.parse(expression, attributeFor)

        //Only return the condition if non-null and holds for the case
        return if (condition == null) {
            ConditionParsingResult(errorMessage = DOES_NOT_CORRESPOND_TO_A_CONDITION)
        } else if (!condition.holds(case)) {
            ConditionParsingResult(errorMessage = CONDITION_IS_NOT_TRUE)
        } else {
            //if this a new condition, the following will store it with its user expression, else the existing condition will be returned
            ConditionParsingResult(conditionManager.getOrCreate(condition))
        }
    }

    private fun checkRuleSessionHistoryConsistency() {
        val idsOfNonRootRulesInTree = ruleTree.rules().filter { it.parent != null }.map { it.id }.toSet()
//        val ruleIdsFromSessions = ruleSessionRecorder.idsOfAllSessionRules()
//        assert(idsOfNonRootRulesInTree == ruleIdsFromSessions) {"Ids of rules in sessions don't match non-root tree rules."}
    }

    internal fun holdsForSessionCase(condition: Condition) = condition.holds(ruleSession!!.case)
    fun conditionForExpression(expression: String) = conditionForExpression(expression, ruleSession!!.case)

    suspend fun startConversation(case: RDRCase): String {
        val chatService = KBChatService.createKBChatService(case)
        val conversationService = Conversation(
            chatService, reasonTransformer =
                object : REASON_TRANSFORMER {
                    override suspend fun transform(reason: String) =
                        conditionForExpression(reason, case).toExpressionTransformation()
                })
        chatManager = ChatManager(conversationService, ruleService)
        return chatManager.startConversation(case)
    }

    suspend fun responseToUserMessage(message: String) = chatManager.response(message)
}