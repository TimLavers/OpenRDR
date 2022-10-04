package io.rippledown.server

import io.rippledown.kb.KB
import io.rippledown.model.*
import io.rippledown.model.condition.Condition
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class ServerApplication {
    val casesDir = File("temp/cases")
    val interpretationsDir = File("temp/interpretations")
    var kb = KB("Thyroids")

    init {
        casesDir.mkdirs()
        interpretationsDir.mkdirs()
    }

    fun waitingCasesInfo(): CasesInfo {
        fun readCaseDetails(file: File): CaseId {
            return CaseId(getCaseFromFile(file).name, getCaseFromFile(file).name)
        }
        val caseFiles = casesDir.listFiles()
        val idsList = caseFiles?.map { file -> readCaseDetails(file) } ?: emptyList()
        return CasesInfo(idsList, casesDir.absolutePath)
    }

    fun createKB() {
        kb = KB("Thyroids")
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

    fun case(id: String): RDRCase {
        val case = getCaseFromFile(File(casesDir, "$id.json"))
        kb.interpret(case)
        return case
    }

    fun saveInterpretation(interpretation: Interpretation): OperationResult {
        val fileName = "${interpretation.caseId.id}.interpretation.json"
        val file = File(interpretationsDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        FileUtils.writeStringToFile(file, Json.encodeToString(interpretation), UTF_8)

        // Now delete the corresponding case file.
        val caseFile = File(casesDir, "${interpretation.caseId.id}.json")
        FileUtils.delete(caseFile)
        return OperationResult("Interpretation submitted")
    }

    private fun getCaseFromFile(file: File): RDRCase {
        val format = Json { allowStructuredMapKeys = true }
        val data = FileUtils.readFileToString(file, UTF_8)
        return format.decodeFromString(data)
    }
}