package io.rippledown.server

import io.rippledown.constants.server.DEFAULT_PROJECT_NAME
import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.CasesInfo
import io.rippledown.model.Conclusion
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.*
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.model.rule.*
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.util.EntityRetrieval
import java.io.File
import kotlin.io.path.createTempDirectory

class ServerApplication(private val persistenceProvider: PersistenceProvider = PostgresPersistenceProvider()) {
    val casesDir = File("cases").apply { mkdirs() }
    val interpretationsDir = File("interpretations").apply { mkdirs() }
    private val kbManager = KBManager(persistenceProvider)

    lateinit var kb: KB

    init {
        createKB()
    }

    fun reCreateKB() {
        val oldKBInfo = kbName()
        createKB()
        kbManager.deleteKB(oldKBInfo)
    }

    fun createKB() {
        val kbInfo = kbManager.createKB(DEFAULT_PROJECT_NAME, true)
        kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
    }

    fun kbName(): KBInfo {
        return kb.kbInfo
    }

    fun exportKBToZip(): File {
        val tempDir: File = createTempDirectory().toFile()
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()
        val file = File(tempDir, "${kb.kbInfo}.zip")
        file.writeBytes(bytes)
        return file
    }

    fun importKBFromZip(zipBytes: ByteArray) {
        val tempDir: File = createTempDirectory().toFile()
        Unzipper(zipBytes, tempDir).unzip()
        val subDirectories = tempDir.listFiles()
        require(subDirectories != null && subDirectories.size == 1) {
            "Invalid zip for KB import."
        }
        val rootDir = subDirectories[0]
        kb = KBImporter(rootDir, persistenceProvider).import()
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

    fun startRuleSessionToAddConclusion(caseId: Long, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToAddConclusion(conclusion))
    }

    private fun startRuleSessionToRemoveConclusion(caseId: Long, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToRemoveConclusion(conclusion))
    }

    fun startRuleSessionToReplaceConclusion(caseId: Long, toGo: Conclusion, replacement: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToReplaceConclusion(toGo, replacement))
    }

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

    fun deleteProcessedCase(name: String) = kb.deletedProcessedCaseWithName(name)

    fun moveAttributeJustBelow(movedId: Int, targetId: Int) {
        val moved = kb.attributeManager.getById(movedId)
        val target = kb.attributeManager.getById(targetId)
        kb.caseViewManager.moveJustBelow(moved, target)
    }

    fun getOrCreateAttribute(name: String) = kb.attributeManager.getOrCreate(name)

    fun getOrCreateConclusion(text: String) = kb.conclusionManager.getOrCreate(text)

    fun getOrCreateCondition(condition: Condition) = kb.conditionManager.getOrCreate(condition)

    /**
     * Save the verified text.
     *
     * @return a ViewableInterpretation with a re-calculated list of Diffs
     * corresponding to difference between the current interpretation and the verified text
     */
    fun saveInterpretation(viewableInterpretation: ViewableInterpretation): ViewableInterpretation {
        require(viewableInterpretation.verifiedText != null) { "Cannot save an unverified interpretation." }

        val caseId = viewableInterpretation.caseId()
        val case = viewableCase(caseId.id!!)

        //persist the verified text and corresponding conclusions associated with the interpretation
        kb.saveInterpretation(viewableInterpretation)

        //reset the case's viewable interpretation including diff list
        val updated = kb.interpretationViewManager.viewableInterpretation(viewableInterpretation.interpretation)

        //update the case in the KB
        case.viewableInterpretation = updated
        //kb.putCase(case) TODO test this for the case where we are using the Postgres persistence provider

        //return the updated interpretation
        return updated
    }

    /**
     * Start a rule session for the given case and difference.
     *
     * @return a CornerstoneStatus providing the first cornerstone and the number of cornerstones that will be affected by the diff
     */
    fun startRuleSession(sessionStartRequest: SessionStartRequest): CornerstoneStatus {
        val caseId = sessionStartRequest.caseId
        val diff = sessionStartRequest.diff

        startRuleSessionForDifference(caseId, diff)
        return kb.cornerstoneStatus(null);

    }

    fun commitRuleSession(ruleRequest: RuleRequest): ViewableInterpretation {
        val caseId = ruleRequest.caseId
        val case = viewableCase(caseId)

        ruleRequest.conditions.conditions.forEach { condition ->
            addConditionToCurrentRuleBuildingSession(condition)
        }
        commitCurrentRuleSession()

        //re-interpret the case
        val updatedInterpretation = kb.interpret(case.case)
        case.viewableInterpretation = kb.interpretationViewManager.viewableInterpretation(updatedInterpretation)

        //return the updated interpretation
        logger.info("Updated interpretation after committing the rule: $updatedInterpretation")
        return case.viewableInterpretation
    }

    private fun uninterpretedCase(id: Long) = kb.getProcessedCase(id)!!

    /**
     * @param cornerstoneIndex the 0-based index of the cornerstone to return
     */
    fun cornerstoneStatusForIndex(cornerstoneIndex: Int): CornerstoneStatus {
        val cornerstones = kb.conflictingCasesInCurrentRuleSession()
        val cornerstone = cornerstones[cornerstoneIndex]
        val viewableCornerstone = kb.viewableCase(cornerstone)
        return CornerstoneStatus(viewableCornerstone, cornerstoneIndex, cornerstones.size)
    }

    fun updateCornerstone(request: UpdateCornerstoneRequest) = kb.updateCornerstone(request)
}
