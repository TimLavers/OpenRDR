package io.rippledown.integration.proxy

import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.awaitility.Awaitility.await
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit

class LabProxy(tempDir: File) {
    private val inputDir = File(tempDir, "cases")
    private val interpretationsDir = File(tempDir, "interpretations")

    fun waitForNumberOfInterpretationsToBe(count: Int) {
        await().atMost(5, TimeUnit.SECONDS).until {
            interpretationsReceived() == count
        }
    }

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