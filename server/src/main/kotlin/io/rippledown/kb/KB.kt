package io.rippledown.kb

import io.rippledown.chat.Conversation
import io.rippledown.chat.ReasonTransformation
import io.rippledown.chat.ReasonTransformer
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.hints.AttributeFor
import io.rippledown.hints.ConditionTip
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
import io.rippledown.server.websocket.WebSocketManager
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking


class KB(persistentKB: PersistentKB, val webSocketManager: WebSocketManager? = null) : RuleService {
    val logger = lazyLogger

    val kbInfo = persistentKB.kbInfo()
    val metaInfo = MetaInfo(persistentKB.metaDataStore())
    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val conclusionManager = ConclusionManager(persistentKB.conclusionStore())
    val conditionManager = ConditionManager(attributeManager, persistentKB.conditionStore())
    val interpretationViewManager = InterpretationViewManager(persistentKB.conclusionOrderStore(), conclusionManager)
    val ruleSessionRecorder = RuleSessionRecorder(persistentKB.ruleSessionRecordStore())
    private val ruleManager = RuleManager(conclusionManager, conditionManager, persistentKB.ruleStore())
    private val caseManager = CaseManager(persistentKB.caseStore(), attributeManager)
    internal val caseViewManager = CaseViewManager(persistentKB.attributeOrderStore(), attributeManager)
    val ruleTree = ruleManager.ruleTree()
    private var ruleSession: RuleBuildingSession? = null

    private var conditionParser: ConditionParser
    private lateinit var chatManager: ChatManager

    init {
        conditionParser = object : ConditionParser {
            override fun parse(expression: String, attributeFor: AttributeFor) =
                ConditionTip(attributeNames(), attributeFor).conditionFor(expression)
        }
    }

    override fun moveAttributeTo(moved: String, destination: String) {
        val attributeMoved = attributeManager.all().first { it.name.equals(moved) }
        val attributeDestination = attributeManager.all().first { it.name.equals(destination) }
        caseViewManager.move(attributeMoved, attributeDestination)
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

    fun startRuleSession(case: RDRCase, action: RuleTreeChange): CornerstoneStatus {
        logger.info("KB starting rule session for case ${case.name} and action $action")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(conclusionManager)
        ruleSession = RuleBuildingSession(ruleManager, ruleTree, case, alignedAction, allCornerstoneCases())
        logger.info("KB rule session created")
        return cornerstoneStatus(null)
    }

    override fun startRuleSessionToAddComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        val conclusion = conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToAddConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        val conclusion = conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToRemoveConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus {
        val replacedConclusion = conclusionManager.getOrCreate(replacedComment)
        val replacementConclusion = conclusionManager.getOrCreate(replacementComment)
        val action = ChangeTreeToReplaceConclusion(replacedConclusion, replacementConclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun sendCornerstoneStatus() {
        val cornerstoneStatus = cornerstoneStatus(null)
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

    fun cancelRuleSession() {
        check(ruleSession != null) { "No rule session in progress." }
        ruleSession = null
    }

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
            require(existing!!.sameAs(condition)) {
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
        checkRuleSessionHistoryConsistency()
    }

    //TODO allow the user to exempt a cornerstone case other than the first
    override fun exemptCornerstoneCase() = exemptCornerstone(0)

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

    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
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

    fun conditionForExpression(expression: String) = conditionForExpression(ruleSession!!.case, expression)

    /**
     * Starts a new conversation for the given viewable case.
     *
     * @param viewableCase The case to start a conversation about
     * @return A string representing the conversation ID or initial response
     */
    suspend fun startConversation(viewableCase: ViewableCase): String {
        val chatService = KBChatService.createKBChatService(viewableCase)
        val conversationService = Conversation(
            chatService = chatService,
            reasonTransformer = createReasonTransformer(viewableCase, this)
        )
        chatManager = ChatManager(conversationService, this)
        return chatManager.startConversation(viewableCase)
    }

    /**
     * Creates a transformer that converts a natural language reason into a rule condition and
     * adds it to the current rule session if it is valid.
     */
    fun createReasonTransformer(viewableCase: ViewableCase, ruleService: RuleService) = object : ReasonTransformer {
        override suspend fun transform(reason: String): ReasonTransformation {
            val result = conditionForExpression(viewableCase.case, reason)
            val condition = result.condition
            if (condition != null) {
                ruleService.addConditionToCurrentRuleSession(condition)
                //inform the UI and the model of the update cornerstones
                //TODO TEST THIS
                val cornerstoneStatus = cornerstoneStatus(null)
                sendCornerstoneStatus()
                chatManager.response(cornerstoneStatus.toJsonString())
            }
            return result.toExpressionTransformation()
        }
    }
    suspend fun responseToUserMessage(message: String) = chatManager.response(message)
}