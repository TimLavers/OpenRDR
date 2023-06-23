package io.rippledown.server

import io.rippledown.kb.KB
import io.rippledown.kb.KBManager
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.diff.*
import io.rippledown.model.rule.*
import io.rippledown.persistence.PersistenceProvider
import io.rippledown.persistence.postgres.PostgresPersistenceProvider
import io.rippledown.util.EntityRetrieval
import io.rippledown.textdiff.diffList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
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
        val kbInfo = kbManager.createKB("Thyroids", true)
        logger.info("Created KB: $kbInfo")
        kb = (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success<KB>).entity
        logger.info("Opened KB:  $kb")
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

    private fun startRuleSessionForDifference(caseId: String, diff: Diff) {
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

    fun startRuleSessionToAddConclusion(caseId: String, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToAddConclusion(conclusion))
    }

    private fun startRuleSessionToRemoveConclusion(caseId: String, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToRemoveConclusion(conclusion))
    }

    fun startRuleSessionToReplaceConclusion(caseId: String, toGo: Conclusion, replacement: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToReplaceConclusion(toGo, replacement))
    }

    fun addConditionToCurrentRuleBuildingSession(condition: Condition) {
        kb.addConditionToCurrentRuleSession(condition)
    }

    fun commitCurrentRuleSession() = kb.commitCurrentRuleSession()

     fun waitingCasesInfo(): CasesInfo {
        fun readCaseDetails(file: File): CaseId {
            synchronized(this) {
                val caseFromFile = getCaseFromFile(file)
                return CaseId(file.nameWithoutExtension, caseFromFile.name)
            }
        }

        val caseFiles = casesDir.listFiles()
        val idsList = caseFiles?.map { file -> readCaseDetails(file) } ?: emptyList()
        return CasesInfo(idsList, casesDir.absolutePath)
    }

    fun case(id: String): RDRCase {
        val case = uninterpretedCase(id)
        kb.interpret(case)
        return case
    }

    fun viewableCase(id: String): ViewableCase {
        return kb.viewableInterpretedCase(uninterpretedCase(id)).apply {
            //reset the case's diff list
            interpretation.diffList = diffList(interpretation)
        }
    }

    fun conditionHintsForCase(id: String): ConditionList = kb.conditionHintsForCase(case(id))

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
     * @return an Interpretation with the list of Diffs corresponding to the changes made to the current interpretation by the verified text
     */
    fun saveInterpretation(interpretation: Interpretation): Interpretation {
        val caseId = interpretation.caseId.id
        val case = case(caseId)

        //reset the case's verified text
        case.interpretation.verifiedText = interpretation.verifiedText

        //reset the case's diff list
        case.interpretation.diffList = diffList(interpretation)

        //update the case in the KB
        kb.putCase(case)

        //return the updated interpretation
        return case.interpretation
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
       val cornerstones = kb.conflictingCasesInCurrentRuleSession()

        logger.info("cornerstones = ${cornerstones.size}")

        return if (cornerstones.isNotEmpty()){
            val cornerstone = cornerstones.first()
            val viewableCornerstone = kb.viewableInterpretedCase(cornerstone)
            CornerstoneStatus(viewableCornerstone, 0, cornerstones.size)
        } else {
            CornerstoneStatus()
        }
    }


    fun commitRuleSession(ruleRequest: RuleRequest): Interpretation {
        val caseId = ruleRequest.caseId
        val case = case(caseId)

        ruleRequest.conditionList.conditions.forEach { condition ->
            addConditionToCurrentRuleBuildingSession(condition)
        }
        commitCurrentRuleSession()

        //re-interpret the case
        kb.interpret(case)

        //reset the case's diff list to account of the updated interpretation
        val updatedInterpretation = case.interpretation
        case.interpretation.diffList = diffList(updatedInterpretation)

        //put the updated case back into the KB
        kb.putCase(case)

        //return the updated interpretation
        return case.interpretation
    }

    private fun writeInterpretationToFile(id: String, interpretation: Interpretation) {
        val fileName = "$id.interpretation.json"
        println("${LocalDateTime.now()}  saving interp = $fileName")
        val file = File(interpretationsDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        FileUtils.writeStringToFile(file, Json.encodeToString(interpretation), UTF_8)
    }

    private fun getCaseFromFile(file: File): RDRCase {
        // The json in the file has attributes with
        // dummy ids. We parse the json into a case
        // and then switch the attributes in it with
        // ones in the KB. When we have a proper
        // external case format, we can do something
        // less confusing.
        val format = Json { allowStructuredMapKeys = true }
        val data = file.readText()
        val caseWithDummyAttributes: RDRCase = format.decodeFromString(data)
        val dataMap = mutableMapOf<TestEvent, TestResult>()
        caseWithDummyAttributes.data.map {
            val originalTestEvent = it.key
            val originalAttribute = originalTestEvent.attribute
            val newAttribute = kb.attributeManager.getOrCreate(originalAttribute.name)
            val newTestEvent = TestEvent(newAttribute, originalTestEvent.date)
            dataMap[newTestEvent] = it.value
        }
        return RDRCase(id = caseWithDummyAttributes.id,  name = caseWithDummyAttributes.name, data = dataMap)
    }

    private fun uninterpretedCase(id: String): RDRCase {
        synchronized(this) {
            if (!kb.containsCaseWithId(id)) {
                //TODO. We assume for now that the id of the case is the same as the name of the file
                val case = getCaseFromFile(File(casesDir, "$id.json"))
                kb.putCase(case)
            }
            return kb.caseForId(id)
        }
    }
}
