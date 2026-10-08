package io.rippledown.server

import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import io.rippledown.kb.report.ReportService
import io.rippledown.log.lazyLogger
import io.rippledown.model.*
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

    fun description() = session.locked { kb.description() }

    fun setDescription(newDescription: String) = session.locked {
        kb.setDescription(newDescription)
    }

    fun descriptionOfMostRecentRule(userId: UserId) =
        session.locked { ruleSessionManager(userId).descriptionOfMostRecentRule() }

    fun undoLastRule(userId: UserId) = session.locked {
        ruleSessionManager(userId).undoLastRuleSession()
    }

    fun exportKBToZip(): File = session.locked {
        val tempDir: File = createTempDirectory().toFile()
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()
        val file = File(tempDir, "${kb.kbInfo}.zip")
        file.writeBytes(bytes)
        file
    }

    fun cancelRuleSession(userId: UserId) = session.locked { ruleSessionManager(userId).cancelRuleSession() }

    fun addConditionToCurrentRuleBuildingSession(condition: Condition, userId: UserId) = session.locked {
        ruleSessionManager(userId).addConditionToCurrentRuleSession(condition)
    }

    fun commitCurrentRuleSession(userId: UserId) =
        session.locked { ruleSessionManager(userId).commitCurrentRuleSession() }

    fun waitingCasesInfo() = session.locked {
        CasesInfo(
            caseIds = kb.processedCaseIds(),
            cornerstoneCaseIds = kb.cornerstoneCaseIds(),
            userDefinedCaseLists = kb.userDefinedCaseLists(),
            kbName = kb.kbInfo.name
        )
    }

    fun case(id: Long): RDRCase = session.locked {
        val case = uninterpretedCase(id)
        kb.interpret(case)
        case
    }

    fun viewableCase(id: Long) = session.locked { kb.viewableCase(uninterpretedCase(id)) }

    fun conditionHintsForCase(id: Long, userId: UserId): ConditionList =
        session.locked { ruleSessionManager(userId).conditionHintsForCase(case(id)) }

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

    fun processCase(externalCase: ExternalCase) = session.locked { kb.processCase(externalCase) }

    fun addCornerstoneCase(externalCase: ExternalCase) = session.locked { kb.addCornerstoneCase(externalCase) }

    fun deleteCase(name: String) = session.locked { kb.deletedProcessedCaseWithName(name) }

    fun moveAttribute(movedId: Int, targetId: Int) = session.locked {
        val moved = kb.attributeManager.getById(movedId)
        val target = kb.attributeManager.getById(targetId)
        kb.caseViewManager.move(moved, target)
    }

    fun getOrCreateAttribute(name: String) = session.locked { kb.attributeManager.getOrCreate(name) }

    fun setAttributeOrder(attributesInOrder: List<Attribute>) =
        session.locked { kb.caseViewManager.set(attributesInOrder) }

    fun getOrCreateCondition(condition: Condition) = session.locked { kb.conditionManager.getOrCreate(condition) }

    fun startRuleSession(request: SessionStartRequest, userId: UserId) =
        session.locked { ruleSessionManager(userId).startRuleSession(request) }

    fun commitRuleSession(request: RuleRequest, userId: UserId) =
        session.locked { ruleSessionManager(userId).commitRuleSession(request) }

    fun uninterpretedCase(id: Long) = session.locked {
        kb.getProcessedCase(id) ?: throw IllegalArgumentException("Case with id $id not found")
    }

    fun updateCornerstone(request: UpdateCornerstoneRequest, userId: UserId) =
        session.locked { ruleSessionManager(userId).updateCornerstone(request) }

    fun selectCornerstone(index: Int, userId: UserId) =
        session.locked { ruleSessionManager(userId).selectCornerstone(index) }

    fun exemptCornerstone(index: Int, userId: UserId) =
        session.locked { ruleSessionManager(userId).exemptCornerstone(index) }

    // Not locked here: the translation inside calls the LLM, so the
    // RuleSessionManager locks only the part that touches the KB.
    fun conditionForExpression(expression: String, userId: UserId) =
        ruleSessionManager(userId).conditionForExpression(expression)

    /**
     * Build a complete rule in one call, without using the UI.
     * Condition expressions are parsed deterministically from human-readable text.
     */
    fun buildRule(request: BuildRuleRequest, userId: UserId) =
        session.locked { ruleSessionManager(userId).buildRule(request) }

    private fun ruleSessionManager(userId: UserId): RuleSessionManager = session.ruleSessionManagerFor(userId)
}
