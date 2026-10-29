package io.rippledown.server

import io.rippledown.kb.*
import io.rippledown.kb.ai.AiBooleanClassifier
import io.rippledown.kb.ai.AiEvaluationService
import io.rippledown.kb.ai.AiInferenceCoordinator
import io.rippledown.kb.ai.GeminiAiBooleanClassifier
import io.rippledown.kb.ai.InterpretationIncompleteException
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import io.rippledown.kb.report.ReportService
import io.rippledown.log.lazyLogger
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.report.CaseReport
import io.rippledown.model.rule.BuildRuleRequest
import io.rippledown.model.rule.RuleRequest
import io.rippledown.model.rule.SessionStartRequest
import io.rippledown.model.rule.UpdateCornerstoneRequest
import java.io.File
import kotlin.io.path.createTempDirectory

class KBEndpoint(
    val session: KBSession,
    private val reportService: ReportService = ReportService(),
    classifier: AiBooleanClassifier = GeminiAiBooleanClassifier()
) {
    val kb get() = session.kb
    val logger = lazyLogger

    /**
     * Obtains AI evaluations outside the KB lock for every operation that
     * interprets a case; see [AiInferenceCoordinator].
     */
    val coordinator by lazy {
        AiInferenceCoordinator(session, AiEvaluationService(kb.aiEvaluationStore, classifier))
    }

    private val reportCache = mutableMapOf<Long, Pair<Int, CaseReport>>() // caseId -> (commentsHash, report)

    fun kbInfo(): KBInfo {
        logger.info("kbName will return: ${kb.kbInfo.name}")
        return kb.kbInfo
    }

    fun description() = session.locked { kb.description() }

    fun setDescription(newDescription: String, userId: UserId) = session.locked {
        kb.setDescription(newDescription)
        kb.changeLog.record(userId, ChangeKind.DESCRIPTION_SET, descriptionSetSummary())
    }

    fun changes(sinceId: Int?, limit: Int) = session.locked { kb.changeLog.since(sinceId).takeLast(limit) }

    fun descriptionOfMostRecentRule(userId: UserId) =
        session.locked { ruleSessionManager(userId).descriptionOfMostRecentRule() }

    fun undoLastRule(userId: UserId) = session.locked {
        ruleSessionManager(userId).undoLastRuleSession()
    }

    fun exportKBToZip(): File {
        val evaluations = kb.aiEvaluationStore.all()
        return session.locked {
            val tempDir: File = createTempDirectory().toFile()
            KBExporter(tempDir, kb, evaluations).export()
            val bytes = Zipper(tempDir).zip()
            val file = File(tempDir, "${kb.kbInfo}.zip")
            file.writeBytes(bytes)
            file
        }
    }

    fun cancelRuleSession(userId: UserId) = session.locked { ruleSessionManager(userId).cancelRuleSession() }

    fun cornerstoneStatus(userId: UserId) = session.locked {
        ruleSessionManager(userId).takeIf { it.isRuleSessionActive() }?.cornerstoneStatus()
    }

    fun addConditionToCurrentRuleBuildingSession(condition: Condition, userId: UserId) = session.locked {
        ruleSessionManager(userId).addConditionToCurrentRuleSession(condition)
    }

    suspend fun commitCurrentRuleSession(userId: UserId) =
        preparedSession(userId, null) { it.commitCurrentRuleSession() }

    fun waitingCasesInfo() = session.locked {
        CasesInfo(
            caseIds = kb.processedCaseIds(),
            cornerstoneCaseIds = kb.cornerstoneCaseIds(),
            userDefinedCaseLists = kb.userDefinedCaseLists(),
            kbName = kb.kbInfo.name
        )
    }

    /**
     * The stored case with its interpretation. AI evaluations happen outside
     * the KB lock; see [AiInferenceCoordinator].
     */
    suspend fun interpretedCase(id: Long): RDRCase = coordinator.interpretStored(id).case.withoutDerivedValues()

    suspend fun viewableCase(id: Long): ViewableCase = try {
        // Interpret and resolve display definitions in the same locked pass.
        prepared(id) { session.locked {
            val live = kb.getCase(id) ?: throw IllegalArgumentException("Case with id $id not found")
            kb.viewableCase(live.copy(interpretation = Interpretation(live.caseId)))
        } }
    } catch (failure: InterpretationIncompleteException) {
        val details = session.locked {
            kb.getCase(id)?.let { live ->
                val primary = live.withoutDerivedValues().copy(interpretation = Interpretation(live.caseId))
                val view = kb.viewableCaseOf(primary)
                failure.details.copy(primaryCase = view.case, caseViewProperties = view.viewProperties)
            } ?: failure.details
        }
        throw InterpretationIncompleteException(details)
    }

    private suspend fun <T> prepared(caseId: Long?, operation: () -> T): T =
        coordinator.withPreparedEvaluations(caseId, operation)

    private suspend fun <T> preparedSession(userId: UserId, caseId: Long?, operation: (RuleSessionManager) -> T): T {
        var guard: (() -> Unit)? = null
        return prepared(caseId) {
            session.locked {
                val manager = ruleSessionManager(userId)
                if (guard == null) guard = manager.operationGuard()
                guard?.invoke()
                operation(manager)
            }
        }
    }

    suspend fun conditionHintsForCase(id: Long, userId: UserId): ConditionList = prepared(id) {
        session.locked {
            val case = uninterpretedCase(id)
            kb.interpret(case)
            ruleSessionManager(userId).conditionHintsForCase(case)
        }
    }

    suspend fun caseReport(caseId: Long): CaseReport {
        val viewable = viewableCase(caseId)
        // Cache invalidates when the case's comments change. latestText() is the
        // already-computed comment text on the viewable interpretation, so we avoid
        // recomputing toComments() here (generate() does its own comment check).
        val key = viewable.viewableInterpretation.latestText().hashCode()
        reportCache[caseId]?.let { (cachedKey, cached) -> if (cachedKey == key) return cached }
        val report = reportService.generate(viewable) { null }
        reportCache[caseId] = key to report
        return report
    }

    /**
     * Stores the case, then interprets it. The primary data is retained even
     * if the interpretation cannot be completed; the exception then names the
     * stored case so that it can be requested again.
     */
    suspend fun processCase(externalCase: ExternalCase): RDRCase {
        val stored = session.locked { kb.storeCase(externalCase) }
        val interpreted = interpretedCase(requireNotNull(stored.id) { "A stored case has an id." })
        // Callers reading the interpretation directly (e.g. the interpreter
        // API) see CommentTemplate/Literal expressions, not sentinels.
        session.locked { interpreted.interpretation.resolveDefinitions(kb.definitionResolver) }
        return interpreted
    }

    fun addCornerstoneCase(externalCase: ExternalCase) = session.locked { kb.addCornerstoneCase(externalCase) }

    fun deleteCase(name: String, userId: UserId) = session.locked {
        if (kb.deletedProcessedCaseWithName(name)) {
            kb.changeLog.record(userId, ChangeKind.CASE_DELETED, caseDeletedSummary(name))
        }
    }

    fun moveAttribute(movedId: Int, targetId: Int, userId: UserId) = session.locked {
        val moved = kb.attributeManager.getById(movedId)
        val target = kb.attributeManager.getById(targetId)
        kb.caseViewManager.move(moved, target)
        kb.changeLog.record(
            userId,
            ChangeKind.ATTRIBUTE_MOVED,
            attributeMovedSummary(moved, target),
            attributeIds = setOf(moved.id, target.id)
        )
    }

    fun getOrCreateAttribute(name: String) = session.locked { kb.attributeManager.getOrCreate(name) }

    fun setAttributeOrder(attributesInOrder: List<Attribute>) =
        session.locked { kb.caseViewManager.set(attributesInOrder) }

    fun getOrCreateCondition(condition: Condition) = session.locked { kb.conditionManager.getOrCreate(condition) }

    suspend fun startRuleSession(request: SessionStartRequest, userId: UserId) =
        preparedSession(userId, request.caseId) { it.startRuleSession(request) }

    suspend fun commitRuleSession(request: RuleRequest, userId: UserId) =
        preparedSession(userId, request.caseId) { it.commitRuleSession(request) }

    fun uninterpretedCase(id: Long) = session.locked {
        kb.getProcessedCase(id) ?: throw IllegalArgumentException("Case with id $id not found")
    }

    suspend fun updateCornerstone(request: UpdateCornerstoneRequest, userId: UserId) =
        preparedSession(userId, null) { it.updateCornerstone(request) }

    suspend fun selectCornerstone(index: Int, userId: UserId) =
        preparedSession(userId, null) { it.selectCornerstone(index) }

    suspend fun exemptCornerstone(index: Int, userId: UserId) =
        preparedSession(userId, null) { it.exemptCornerstone(index) }

    // Not locked here: the translation inside calls the LLM, so the
    // RuleSessionManager locks only the part that touches the KB.
    suspend fun conditionForExpression(expression: String, userId: UserId): io.rippledown.model.condition.ConditionParsingResult {
        val manager = ruleSessionManager(userId)
        val guard = session.locked { manager.operationGuard() }
        return prepared(null) {
            session.locked { guard() }
            val result = manager.conditionForExpression(expression)
            session.locked { guard(); result }
        }
    }

    /**
     * Build a complete rule in one call, without using the UI.
     * Condition expressions are parsed deterministically from human-readable text.
     */
    suspend fun buildRule(request: BuildRuleRequest, userId: UserId) =
        preparedSession(userId, null) { it.buildRule(request) }

    private fun ruleSessionManager(userId: UserId): RuleSessionManager = session.ruleSessionManagerFor(userId)
}
