package io.rippledown.server

import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class ServerApplication {
    val casesDir = File("temp/cases")
    val interpretationsDir = File("temp/interpretations")

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

    fun case(id: String): RDRCase {
        return getCaseFromFile(File(casesDir, "$id.json"))
    }
    fun saveInterpretation(interpretation: Interpretation) {
        val fileName = "${interpretation.caseId.id}.interpretation.json"
        val file = File(interpretationsDir, fileName)
        if (file.exists()) {
            file.delete()
        }
        FileUtils.writeStringToFile(file, Json.encodeToString(interpretation), UTF_8)

        // Now delete the corresponding case file.
        val caseFile = File(casesDir, "${interpretation.caseId.id}.json")
        FileUtils.delete(caseFile)
    }

    private fun getCaseFromFile(file: File): RDRCase {
        val data = FileUtils.readFileToString(file, UTF_8)
        return Json.decodeFromString(data)
    }
}