package io.rippledown.integration.labsystem

import io.rippledown.CaseTestUtils
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class LabServerProxy {
    private val inputDir = File("temp/cases")
    private val interpretationsDir = File("temp/interpretations")

    fun interpretationsReceived(): Int {
        return interpretationsDir.listFiles()!!.size
    }

    fun interpretationReceived(caseName: String): String {
        val file = File(interpretationsDir, "$caseName.interpretation.json")
        val data = FileUtils.readFileToString(file, UTF_8)
        val interpretation: Interpretation = Json.decodeFromString(data)
        return interpretation.text
    }

    fun inputCases(): Set<String> {
        return inputDir.list()!!.map { s -> s.dropLast(5) }.toCollection(mutableSetOf())
    }

    fun cleanCasesDir() {
        FileUtils.cleanDirectory(inputDir)
    }

    fun cleanInterpretationsDir() {
        FileUtils.cleanDirectory(interpretationsDir)
    }

    fun copyCase(caseName: String) {
        FileUtils.copyFileToDirectory(CaseTestUtils.caseFile(caseName), inputDir)
    }

    fun writeCaseToInputDir(rdrCase: RDRCase) {
        val file = File(inputDir, "${rdrCase.name}.json")
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(rdrCase)
        FileUtils.writeStringToFile(file, serialized, UTF_8)
    }
}