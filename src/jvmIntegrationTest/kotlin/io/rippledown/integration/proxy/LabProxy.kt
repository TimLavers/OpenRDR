package io.rippledown.integration.proxy

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Interpretation
import io.rippledown.model.RDRCase
import io.rippledown.model.TestResult
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.awaitility.Awaitility.await
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.concurrent.TimeUnit

class LabProxy(tempDir: File, private val restProxy: RESTClient) {
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
        return interpretation.verifiedText ?: ""
    }

    fun cleanCasesDir() {
        if (!inputDir.exists()) {
            inputDir.mkdirs()
        }
        FileUtils.cleanDirectory(inputDir)
    }

    fun provideCase(caseName: String) {
        val data = readCaseData(caseName)
        provideCaseFromString(data)
    }

    private fun readCaseData(caseName: String) = ConfiguredTestData.caseFile(caseName).readText()

    private fun provideCaseFromString(data: String) {
        val jsonBuilder = Json { allowStructuredMapKeys = true }
        val case: ExternalCase = jsonBuilder.decodeFromString(data)
        restProxy.provideCase(case)
    }

    fun provideCaseWithName(caseName: String) {
        val data = readCaseData("Case1")
        val toSend = data.replace("Case1", caseName)
        provideCaseFromString(toSend)
    }

    fun deleteCase(caseName: String) {
        val deleted = File(inputDir, "${caseName}.json").delete()
        if (!deleted) {
            throw IllegalStateException("Could not delete case $caseName")
        }
    }

    fun provideCase(rdrCase: RDRCase) {
        // For now we convert the RDRCase into an ExternalCase
        // and provide that. TODO change this - make the parameter an ExternalCase
        val data = mutableMapOf<MeasurementEvent, TestResult>()
        rdrCase.data.forEach { data[MeasurementEvent(it.key.attribute.name, it.key.date)] = it.value }
        restProxy.provideCase(ExternalCase(rdrCase.name, data))
    }

    fun provideCase(name: String, attributeNameToValue: Map<String, String>) {
        val now = Instant.now().toEpochMilli()
        val data = mutableMapOf<MeasurementEvent, TestResult>()
        attributeNameToValue.forEach{  data[MeasurementEvent(it.key, now)] =  TestResult(it.value) }
        val case = ExternalCase(name, data)
        restProxy.provideCase(case)
    }
}