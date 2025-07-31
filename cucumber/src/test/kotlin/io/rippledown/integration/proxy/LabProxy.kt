package io.rippledown.integration.proxy

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.RDRCase
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import kotlinx.serialization.json.Json
//import org.apache.commons.io.FileUtils
import java.io.File
import java.time.Instant.now

class LabProxy(tempDir: File, val restProxy: RESTClient) {
    private val inputDir = File(tempDir, "cases")

    fun cleanCasesDir() {
        if (!inputDir.exists()) {
            inputDir.mkdirs()
        }
//        FileUtils.cleanDirectory(inputDir)
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

    fun provideCase(rdrCase: RDRCase) {
        // For now we convert the RDRCase into an ExternalCase
        // and provide that. TODO change this - make the parameter an ExternalCase
        val data = mutableMapOf<MeasurementEvent, TestResult>()
        rdrCase.data.forEach { data[MeasurementEvent(it.key.attribute.name, it.key.date)] = it.value }
        restProxy.provideCase(ExternalCase(rdrCase.name, data))
    }

    fun provideCase(name: String, attributeNameToValue: Map<String, String>) {
        val now = now().toEpochMilli()
        val data = mutableMapOf<MeasurementEvent, TestResult>()
        attributeNameToValue.forEach { data[MeasurementEvent(it.key, now)] = TestResult(it.value) }
        val case = ExternalCase(name, data)
        restProxy.provideCase(case)
    }

    fun provideCaseForKb(kbName: String, caseName: String, attributeNameToValue: Map<String, String>): RDRCase {
        val now = now().toEpochMilli()
        val data = mutableMapOf<MeasurementEvent, TestResult>()
        attributeNameToValue.forEach { data[MeasurementEvent(it.key, now)] = TestResult(it.value) }
        val case = ExternalCase(caseName, data)
        return restProxy.provideCaseForKB(kbName, case)
    }

    fun provideCase(caseName: String, details: List<TestResultDetail>) {
        val now = now().toEpochMilli()
        val data = details.associate {
            val referenceRange = ReferenceRange(it.lowReferenceRange, it.highReferenceRange)
            MeasurementEvent(it.attributeName, now) to TestResult(it.result, referenceRange, it.units)
        }
        val case = ExternalCase(caseName, data)
        restProxy.provideCase(case)
    }

}

data class TestResultDetail(
    val attributeName: String,
    val result: String,
    val lowReferenceRange: String?,
    val highReferenceRange: String?,
    val units: String?
)
