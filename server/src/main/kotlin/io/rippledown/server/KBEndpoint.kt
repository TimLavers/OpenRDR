package io.rippledown.server

import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import io.rippledown.kb.report.ReportService
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.chat.ChatResponse
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
    private val reportService: ReportService = ReportService()
) {
    val kb get() = session.kb
    val logger = lazyLogger

    private val reportCache = mutableMapOf<Long, Pair<Int, CaseReport>>() // caseId -> (commentsHash, report)

    fun kbInfo(): KBInfo {
        logger.info("kbName will return: ${kb.kbInfo.name}")
        return kb.kbInfo
    }

    fun description() = kb.description()

    fun setDescription(newDescription: String) {
        kb.setDescription(newDescription)
    }

    fun descriptionOfMostRecentRule() = ruleSessionManager().descriptionOfMostRecentRule()

    fun undoLastRule() {
        ruleSessionManager().undoLastRuleSession()
    }

    fun exportKBToZip(): File {
        val tempDir: File = createTempDirectory().toFile()
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()
        val file = File(tempDir, "${kb.kbInfo}.zip")
        file.writeBytes(bytes)
        return file
    }

    fun cancelRuleSession() = ruleSessionManager().cancelRuleSession()

    fun addConditionToCurrentRuleBuildingSession(condition: Condition) {
        ruleSessionManager().addConditionToCurrentRuleSession(condition)
    }

    fun commitCurrentRuleSession() = ruleSessionManager().commitCurrentRuleSession()

    fun waitingCasesInfo() = CasesInfo(
        caseIds = kb.processedCaseIds(),
        cornerstoneCaseIds = kb.cornerstoneCaseIds(),
        kbName = kb.kbInfo.name
    )

    fun case(id: Long): RDRCase {
        val case = uninterpretedCase(id)
        kb.interpret(case)
        return case
    }

    fun viewableCase(id: Long) = kb.viewableCase(uninterpretedCase(id))

    fun conditionHintsForCase(id: Long): ConditionList = ruleSessionManager().conditionHintsForCase(case(id))

    suspend fun startConversation(caseId: Long): ChatResponse = session.startConversation(viewableCase(caseId))

    suspend fun responseToUserMessage(message: String): ChatResponse = session.responseToUserMessage(message)

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

    fun processCase(externalCase: ExternalCase) = kb.processCase(externalCase)

    fun addCornerstoneCase(externalCase: ExternalCase) = kb.addCornerstoneCase(externalCase)

    fun deleteCase(name: String) = kb.deletedProcessedCaseWithName(name)

    fun moveAttribute(movedId: Int, targetId: Int) {
        val moved = kb.attributeManager.getById(movedId)
        val target = kb.attributeManager.getById(targetId)
        kb.caseViewManager.move(moved, target)
    }

    fun getOrCreateAttribute(name: String) = kb.attributeManager.getOrCreate(name)

    fun setAttributeOrder(attributesInOrder: List<Attribute>) = kb.caseViewManager.set(attributesInOrder)

    fun getOrCreateCondition(condition: Condition) = kb.conditionManager.getOrCreate(condition)

    fun startRuleSession(request: SessionStartRequest) = ruleSessionManager().startRuleSession(request)

    fun commitRuleSession(request: RuleRequest) = ruleSessionManager().commitRuleSession(request)

    fun uninterpretedCase(id: Long) = kb.getProcessedCase(id) ?: throw IllegalArgumentException("Case with id $id not found")

    fun updateCornerstone(request: UpdateCornerstoneRequest) = ruleSessionManager().updateCornerstone(request)
    fun selectCornerstone(index: Int) = ruleSessionManager().selectCornerstone(index)
    fun exemptCornerstone(index: Int) = ruleSessionManager().exemptCornerstone(index)
    fun conditionForExpression(expression: String) = ruleSessionManager().conditionForExpression(expression)

    /**
     * Build a complete rule in one call, without using the UI.
     * Condition expressions are parsed deterministically from human-readable text.
     */
    fun buildRule(request: BuildRuleRequest) = ruleSessionManager().buildRule(request)

    private fun ruleSessionManager(): RuleSessionManager = session.ruleSessionManager
}
