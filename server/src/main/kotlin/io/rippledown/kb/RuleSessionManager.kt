package io.rippledown.kb

import io.rippledown.constants.rule.CONDITION_IS_NOT_TRUE
import io.rippledown.constants.rule.DOES_NOT_CORRESPOND_TO_A_CONDITION
import io.rippledown.constants.rule.INTERPRETED_CONDITION_IS_NOT_TRUE
import io.rippledown.hints.AttributeFor
import io.rippledown.hints.ConditionChatService
import io.rippledown.hints.ConditionGenerator
import io.rippledown.kb.chat.RuleService
import io.rippledown.log.lazyLogger
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.rule.*
import io.rippledown.server.ConditionExpressionParser
import io.rippledown.server.websocket.WebSocketManager
import kotlinx.coroutines.runBlocking

class RuleSessionManager(
    private val kb: KB,
    private val webSocketManager: WebSocketManager? = null
) : RuleService {
    val logger = lazyLogger

    private var ruleSession: RuleBuildingSession? = null
    internal var currentDiff: Diff? = null
    private var selectedCornerstone: ViewableCase? = null
    private val conditionChatService = ConditionChatService()
    private var conditionParser: ConditionParser

    init {
        conditionParser = object : ConditionParser {
            override fun parse(expression: String, attributeFor: AttributeFor) =
                ConditionGenerator(attributeFor, conditionChatService, kb.attributeNames()).conditionFor(expression)
        }
    }

    fun startRuleSession(
        case: RDRCase,
        action: RuleTreeChange
    ): CornerstoneStatus {
        logger.info("Starting rule session for case ${case.name} and action $action")
        check(ruleSession == null) { "Session already in progress." }
        check(action.isApplicable(kb.ruleTree, case)) { "Action $action is not applicable to case ${case.name}" }
        val alignedAction = action.alignWith(kb.conclusionManager)
        ruleSession = RuleBuildingSession(kb.ruleManager, kb.ruleTree, case, alignedAction, kb.allCornerstoneCases())
        logger.info("Rule session created")
        return cornerstoneStatus(null)
    }

    override fun startRuleSessionToAddComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        currentDiff = Addition(comment)
        val conclusion = kb.conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToAddConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToRemoveComment(viewableCase: ViewableCase, comment: String): CornerstoneStatus {
        currentDiff = Removal(comment)
        val conclusion = kb.conclusionManager.getOrCreate(comment)
        val action = ChangeTreeToRemoveConclusion(conclusion)
        return startRuleSession(viewableCase.case, action)
    }

    override fun startRuleSessionToReplaceComment(
        viewableCase: ViewableCase,
        replacedComment: String,
        replacementComment: String
    ): CornerstoneStatus {
        currentDiff = Replacement(replacedComment, replacementComment)
        val replacedConclusion = kb.conclusionManager.getOrCreate(replacedComment)
        val replacementConclusion = kb.conclusionManager.getOrCreate(replacementComment)
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
        val condition = kb.conditionManager.getById(conditionId)
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
            kb.conditionManager.getOrCreate(condition)
        } else {
            val existing = kb.conditionManager.getById(condition.id!!)
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
        kb.ruleSessionRecorder.recordRuleSessionCommitted(rulesAdded)
        kb.addCornerstoneCase(ruleSession!!.case)
        ruleSession = null
        currentDiff = null
        checkRuleSessionHistoryConsistency()
        val casesInfo = CasesInfo(
            caseIds = kb.processedCaseIds(),
            cornerstoneCaseIds = kb.cornerstoneCaseIds(),
            kbName = kb.kbInfo.name
        )
        runBlocking { webSocketManager?.sendCasesInfo(casesInfo) }
    }

    override fun exemptCornerstoneCase() = exemptCornerstone(cornerstoneStatus().indexOfCornerstoneToReview)

    override fun selectCornerstoneCase(index: Int) = selectCornerstone(index)

    fun descriptionOfMostRecentRule(): UndoRuleDescription {
        val record = kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()
            ?: return UndoRuleDescription("There are no rules to undo.", false)
        val idOfExemplar = record.idsOfRulesAddedInSession.random()
        val exemplar = kb.ruleTree.ruleForId(idOfExemplar)
        return UndoRuleDescription(exemplar.actionSummary(), true)
    }

    fun ruleSessionHistories() = kb.ruleSessionRecorder.allRuleSessionHistories()

    override fun undoLastRuleSession() {
        val record = kb.ruleSessionRecorder.idsOfRulesAddedInMostRecentSession()!!
        record.idsOfRulesAddedInSession.forEach {
            val toDelete = kb.ruleTree.ruleForId(it)
            kb.ruleManager.deleteLeafRule(toDelete)
        }
        kb.ruleSessionRecorder.delete(kb.ruleSessionRecorder.allRuleSessionHistories().last())
    }

    private fun checkSession() {
        logger.debug("checking session")
        check(ruleSession != null) { "Rule session not started." }
    }

    override fun conditionHintsForCase(case: RDRCase): ConditionList {
        val suggester = ConditionSuggester(kb.attributeManager.all(), case)
        return ConditionList(suggester.suggestions())
    }

    override fun conditionForSuggestionText(case: RDRCase, conditionText: String): Condition? {
        return conditionHintsForCase(case).suggestions
            .firstOrNull { !it.isEditable() && it.asText() == conditionText }
            ?.initialSuggestion()
    }

    override fun currentRuleSessionConditionTexts(): Set<String> {
        return ruleSession?.conditions?.map { it.asText() }?.toSet() ?: emptySet()
    }

    override fun isRuleSessionActive(): Boolean = ruleSession != null

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

        val currentCornerstones = ruleSession!!.cornerstoneCases()
        if (index < 0 || currentCornerstones.isEmpty()) {
            selectedCornerstone = null
            return CornerstoneStatus()
        }
        val toExempt = currentCornerstones[index]
        ruleSession!!.exemptCornerstone(toExempt)

        val cornerstones = ruleSession!!.cornerstoneCases()
        return if (cornerstones.isEmpty()) {
            selectedCornerstone = null
            CornerstoneStatus()
        } else {
            val newCC = cornerstones[index.coerceAtMost(cornerstones.size - 1)]
            val viewable = viewableCase(newCC)
            selectedCornerstone = viewable
            cornerstoneStatus(viewable)
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
        val viewable = viewableCase(newCC)
        selectedCornerstone = viewable
        return CornerstoneStatus(viewable, index, cornerstones.size)
    }

    override fun cornerstoneStatus(): CornerstoneStatus = cornerstoneStatus(selectedCornerstone)

    /**
     * @return the CornerstoneStatus for the current session where the specified cornerstone should remain selected if it is still in the list of cornerstones
     */
    internal fun cornerstoneStatus(currentCornerstone: ViewableCase?): CornerstoneStatus {
        checkSession()
        val cornerstones: List<RDRCase> = ruleSession!!.cornerstoneCases()
        val conditionTexts = ruleSession!!.conditions.map { it.asText() }
        if (cornerstones.isEmpty()) return CornerstoneStatus(diff = currentDiff, ruleConditions = conditionTexts)

        //if no cornerstone has been selected yet, or the selected cornerstone is no longer in the list of cornerstones, return the first one
        var index = 0
        if (currentCornerstone != null) {
            index = cornerstones.indexOf(currentCornerstone.case)
        }
        index = if (index >= 0) index else 0
        val cornerstone = cornerstones[index]
        val viewableCornerstone = kb.viewableCase(cornerstone)
        return CornerstoneStatus(viewableCornerstone, index, cornerstones.size, currentDiff, conditionTexts)
    }

    //Allow a mock parser to be set so we can avoid connecting to Gemini for all the tests
    fun setConditionParser(parser: ConditionParser) {
        conditionParser = parser
    }

    override fun conditionForExpression(case: RDRCase, expression: String): ConditionParsingResult {
        val attributeFor: AttributeFor = { kb.attributeManager.getOrCreate(it) }
        val condition = conditionParser.parse(expression, attributeFor)

        //Only return the condition if non-null and holds for the case
        val caseAttributeNames = case.attributes.map { it.name }.toSet()
        return if (condition == null) {
            ConditionParsingResult(errorMessage = DOES_NOT_CORRESPOND_TO_A_CONDITION)
        } else if (condition.attributeNames().any { it !in caseAttributeNames }) {
            ConditionParsingResult(errorMessage = DOES_NOT_CORRESPOND_TO_A_CONDITION)
        } else if (!condition.holds(case)) {
            val message = if (expression.normalizeForComparison() != condition.asText().normalizeForComparison()) {
                INTERPRETED_CONDITION_IS_NOT_TRUE.format(expression, condition.asText())
            } else {
                CONDITION_IS_NOT_TRUE
            }
            ConditionParsingResult(errorMessage = message)
        } else {
            //if this a new condition, the following will store it with its user expression, else the existing condition will be returned
            ConditionParsingResult(kb.conditionManager.getOrCreate(condition))
        }
    }

    private fun checkRuleSessionHistoryConsistency() {
        val idsOfNonRootRulesInTree = kb.ruleTree.rules().filter { it.parent != null }.map { it.id }.toSet()
    }

    fun conditionForExpression(expression: String) = conditionForExpression(ruleSession!!.case, expression)

    private fun viewableCase(case: RDRCase): ViewableCase {
        return kb.viewableCase(case)
    }

    override fun moveAttributeTo(moved: String, destination: String) {
        val attributeMoved = kb.attributeManager.all().first { it.name.equals(moved) }
        val attributeDestination = kb.attributeManager.all().first { it.name.equals(destination) }
        kb.caseViewManager.move(attributeMoved, attributeDestination)
    }

    fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus {
        logger.info("startRuleSession with data $sessionStartRequest")
        val caseId = sessionStartRequest.caseId
        val diff = sessionStartRequest.diff
        currentDiff = diff
        val case = kb.getProcessedCase(caseId) ?: throw IllegalArgumentException("Case with id $caseId not found")
        kb.interpret(case)
        return when (diff) {
            is Addition -> startRuleSession(case, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate(diff.right())))
            is Removal -> startRuleSession(case, ChangeTreeToRemoveConclusion(kb.conclusionManager.getOrCreate(diff.left())))
            is Replacement -> startRuleSession(
                case,
                ChangeTreeToReplaceConclusion(
                    kb.conclusionManager.getOrCreate(diff.left()),
                    kb.conclusionManager.getOrCreate(diff.right())
                )
            )
        }
    }

    fun commitRuleSession(ruleRequest: RuleRequest): ViewableCase {
        logger.info("Committing rule session for $ruleRequest")
        val caseId = ruleRequest.caseId
        val case = kb.viewableCase(
            kb.getProcessedCase(caseId) ?: throw IllegalArgumentException("Case with id $caseId not found")
        )
        ruleRequest.conditions.conditions.forEach { condition ->
            logger.info("adding condition: $condition")
            addConditionToCurrentRuleSession(condition)
        }
        commitCurrentRuleSession()
        logger.info("rule session committed")
        val updatedInterpretation = kb.interpret(case.case)
        case.viewableInterpretation = kb.interpretationViewManager.viewableInterpretation(updatedInterpretation)
        logger.info("Updated interpretation after committing the rule: $updatedInterpretation")
        return case
    }

    /**
     * Build a complete rule in one call, without using the UI.
     * Condition expressions are parsed deterministically from human-readable text.
     */
    fun buildRule(request: BuildRuleRequest) {
        logger.info("buildRule: case='${request.caseName}', diff=${request.diff}, conditions=${request.conditions}")
        val case = kb.getProcessedCaseByName(request.caseName)
        kb.interpret(case)
        val viewableCase = kb.viewableCase(case)
        when (val diff = request.diff) {
            is Addition -> startRuleSessionToAddComment(viewableCase, diff.addedText)
            is Removal -> startRuleSessionToRemoveComment(viewableCase, diff.removedText)
            is Replacement -> startRuleSessionToReplaceComment(viewableCase, diff.originalText, diff.replacementText)
        }
        try {
            val parser = ConditionExpressionParser { kb.attributeManager.getOrCreate(it) }
            request.conditions.forEach { expression ->
                val condition = parser.parse(expression)
                addConditionToCurrentRuleSession(condition)
            }
            commitCurrentRuleSession()
            logger.info("buildRule: completed for case='${request.caseName}'")
        } catch (e: Exception) {
            logger.error("buildRule: failed for case='${request.caseName}': ${e.message}")
            cancelRuleSession()
            throw e
        }
    }
}
