package io.rippledown.server

import io.rippledown.kb.KBSession
import io.rippledown.kb.RuleSessionManager
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import io.rippledown.log.lazyLogger
import io.rippledown.model.*
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.*
import java.io.File
import kotlin.io.path.createTempDirectory

class KBEndpoint(val session: KBSession) {
    val kb get() = session.kb
    val logger = lazyLogger

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

    fun startRuleSessionToAddConclusion(caseId: Long, conclusion: Conclusion) {
        ruleSessionManager().startRuleSession(case(caseId), ChangeTreeToAddConclusion(conclusion))
    }

    fun startRuleSessionToRemoveConclusion(caseId: Long, conclusion: Conclusion) {
        ruleSessionManager().startRuleSession(case(caseId), ChangeTreeToRemoveConclusion(conclusion))
    }

    fun startRuleSessionToReplaceConclusion(caseId: Long, toGo: Conclusion, replacement: Conclusion) {
        ruleSessionManager().startRuleSession(case(caseId), ChangeTreeToReplaceConclusion(toGo, replacement))
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

    fun getOrCreateConclusion(text: String) = kb.conclusionManager.getOrCreate(text)

    fun allConclusions() = kb.conclusionManager.all()

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
