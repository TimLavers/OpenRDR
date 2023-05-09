package io.rippledown.server

import io.rippledown.kb.KB
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.KBImporter
import io.rippledown.kb.export.util.Unzipper
import io.rippledown.kb.export.util.Zipper
import io.rippledown.model.*
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.textdiff.diffList
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.LocalDateTime
import kotlin.io.path.createTempDirectory

class ServerApplication {
    val casesDir = File("cases").apply { mkdirs() }
    val interpretationsDir = File("interpretations").apply { mkdirs() }
    private val idToCase = mutableMapOf<String, RDRCase>()

    var kb = KB("Thyroids")

    fun createKB() {
        kb = KB("Thyroids")
    }

    fun kbName(): KBInfo {
        return KBInfo(kb.name)
    }

    fun exportKBToZip(): File {
        val tempDir: File = createTempDirectory().toFile()
        KBExporter(tempDir, kb).export()
        val bytes = Zipper(tempDir).zip()
        val file = File(tempDir, "${kb.name}.zip")
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
        kb = KBImporter(rootDir).import()
    }

    fun startRuleSessionToAddConclusion(caseId: String, conclusion: Conclusion) {
        kb.startRuleSession(case(caseId), ChangeTreeToAddConclusion(conclusion))
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
            return CaseId(getCaseFromFile(file).name, getCaseFromFile(file).name)
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

    fun moveAttributeJustBelow(moved: Attribute, target: Attribute) {
        kb.caseViewManager.moveJustBelow(moved, target)
    }

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

        //put the updated case back into the map
        idToCase[caseId] = case

        writeInterpretationToFile(caseId, interpretation)

        //return the updated interpretation
        return case.interpretation
    }

    fun buildRule(interpretation: Interpretation): Interpretation {
        val caseId = interpretation.caseId.id
        val case = case(caseId)

        val diff = interpretation.selectedChange()
        startRuleSessionToAddConclusion(caseId, Conclusion(diff.right()))
        commitCurrentRuleSession()

        kb.interpret(case)

        //reset the case's diff list
        case.interpretation.diffList = diffList(interpretation)

        //put the updated case back into the map
        idToCase[caseId] = case

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
        val format = Json { allowStructuredMapKeys = true }
        val data = FileUtils.readFileToString(file, UTF_8)
        return format.decodeFromString(data)
    }

    internal fun uninterpretedCase(id: String): RDRCase {
        if (!idToCase.containsKey(id)) {
            idToCase[id] = getCaseFromFile(File(casesDir, "$id.json"))
        }
        return idToCase.get(id)!!
    }
}

