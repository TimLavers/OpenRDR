package io.rippledown.integration.proxy

import io.rippledown.model.AttributeFactory
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

class LabProxy(tempDir: File, private val attributeFactory: AttributeFactory) {
    private val inputDir = File(tempDir, "cases")

    fun cleanCasesDir() {
        if (!inputDir.exists()) {
            inputDir.mkdirs()
        }
        FileUtils.cleanDirectory(inputDir)
    }

    fun copyCase(caseName: String) {
        FileUtils.copyFileToDirectory(ConfiguredTestData.caseFile(caseName), inputDir)
    }

    fun writeNewCaseFile(caseName: String) = CaseTestUtils.writeNewCaseFileToDirectory(caseName, inputDir)

    fun deleteCase(caseName: String) {
        val deleted = File(inputDir, "${caseName}.json").delete()
        if (!deleted) {
            throw IllegalStateException("Could not delete case $caseName")
        }
    }

    fun writeCaseToInputDir(rdrCase: RDRCase) {
        val file = File(inputDir, "${rdrCase.name}.json")
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(rdrCase)
        FileUtils.writeStringToFile(file, serialized, UTF_8)
    }

    fun writeCaseWithDataToInputDir(name: String, attributeNameToValue: Map<String, String>) {
        with (RDRCaseBuilder()) {
            val now = Instant.now().toEpochMilli()
            attributeNameToValue.forEach { (a, v) -> addResult(attributeFactory.create(a), now, TestResult(v)) }
            val case = build(name, name)
            writeCaseToInputDir(case)
        }
    }
}