package io.rippledown.server

import io.rippledown.kb.KB
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.*
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.rule.*
import java.io.File
import kotlin.io.path.createTempDirectory

class KBEndpoint(val kb: KB, casesRootDirectory: File) {
    val casesDir = File(casesRootDirectory,"cases").apply { mkdirs() }
    val interpretationsDir = File(casesRootDirectory, "interpretations").apply { mkdirs() }

    fun kbName(): KBInfo {
        logger.info("kbName will return: ${kb.kbInfo.name}")
        return kb.kbInfo
    }

    private fun startRuleSessionForDifference(caseId: Long, diff: Diff) {
        when (diff) {
            is Addition -> startRuleSessionToAddConclusion(caseId, kb.conclusionManager.getOrCreate(diff.right()))
            is Removal -> startRuleSessionToRemoveConclusion(caseId, kb.conclusionManager.getOrCreate(diff.left()))
            is Replacement -> startRuleSessionToReplaceConclusion(
                caseId,
                kb.conclusionManager.getOrCreate(diff.left()),
                kb.conclusionManager.getOrCreate(diff.right())
            )
            is Unchanged -> {}
        }
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
        kb.startRuleSession(case(caseId), ChangeTreeToAddConclusion(conclusion))
    }

    fun startRuleSessionToRemoveConclusion(caseId: Long, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToRemoveConclusion(conclusion))
    }

    fun startRuleSessionToReplaceConclusion(caseId: Long, toGo: Conclusion, replacement: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToReplaceConclusion(toGo, replacement))
    }

    fun cancelRuleSession() = kb.cancelRuleSession()

    fun addConditionToCurrentRuleBuildingSession(condition: Condition) {
        kb.addConditionToCurrentRuleSession(condition)
    }

    fun commitCurrentRuleSession() = kb.commitCurrentRuleSession()

    fun waitingCasesInfo() = CasesInfo(kb.processedCaseIds(), kb.kbInfo.name)

    fun case(id: Long): RDRCase {
        val case = uninterpretedCase(id)
        kb.interpret(case)
        return case
    }

    fun viewableCase(id: Long) = kb.viewableCase(uninterpretedCase(id))

    fun conditionHintsForCase(id: Long): ConditionList = kb.conditionHintsForCase(case(id))

    fun processCase(externalCase: ExternalCase) = kb.processCase(externalCase)

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

    /**
     * Start a rule session for the given case and difference.
     *
     * @return a CornerstoneStatus providing the first cornerstone and the number of cornerstones that will be affected by the diff
     */
    fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus {
        logger.info("startRuleSession with data $sessionStartRequest")
        val caseId = sessionStartRequest.caseId
        val diff = sessionStartRequest.diff
        startRuleSessionForDifference(caseId, diff)
        return kb.cornerstoneStatus(null)
    }

    fun commitRuleSession(ruleRequest: RuleRequest): ViewableCase {
        logger.info("Committing rule session for $ruleRequest")
        val caseId = ruleRequest.caseId
        val case = viewableCase(caseId)

        ruleRequest.conditions.conditions.forEach { condition ->
            logger.info("adding condition: $condition")
            addConditionToCurrentRuleBuildingSession(condition)
        }
        commitCurrentRuleSession()
        logger.info("rule session committed")

        //re-interpret the case
        val updatedInterpretation = kb.interpret(case.case)
        case.viewableInterpretation = kb.interpretationViewManager.viewableInterpretation(updatedInterpretation)

        //return the updated interpretation
        logger.info("Updated interpretation after committing the rule: $updatedInterpretation")
        return case
    }

    private fun uninterpretedCase(id: Long) = kb.getProcessedCase(id)!!

    /**
     * @param cornerstoneIndex the 0-based index of the cornerstone to return
     */
    fun cornerstoneForIndex(cornerstoneIndex: Int): ViewableCase {
        logger.info("about to get cc for index $cornerstoneIndex")
        val cornerstones = kb.conflictingCasesInCurrentRuleSession()
        logger.info("got cc for index          $cornerstoneIndex")
        return kb.viewableCase(cornerstones[cornerstoneIndex])
    }

    fun updateCornerstone(request: UpdateCornerstoneRequest) = kb.updateCornerstone(request)
    fun exemptCornerstone(index: Int) = kb.exemptCornerstone(index)
}
