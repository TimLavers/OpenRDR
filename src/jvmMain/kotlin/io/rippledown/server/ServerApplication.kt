package io.rippledown.server

import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

class ServerApplication {
    val casesDir = File("temp/cases")

    init {
        casesDir.mkdirs()
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

    private fun getCaseFromFile(file: File): RDRCase {
        val data = FileUtils.readFileToString(file, StandardCharsets.UTF_8)
        return Json.decodeFromString(data)
    }
}