package io.rippledown

import io.rippledown.model.*
import io.rippledown.model.external.ExternalCase
import io.rippledown.server.ServerApplication
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

fun setUpCaseFromFile(caseName: String, app: ServerApplication): RDRCase {
    val externalCase = CaseTestUtils.getCase(caseName)
    return app.supplyCase(externalCase)
}

internal object CaseTestUtils {
    private val jsonParser = Json { allowStructuredMapKeys = true }
    private val glucose = Attribute(-200, "Glucose")

    fun getCase(caseName: String): ExternalCase {
        val data = caseData(caseName)
        return jsonParser.decodeFromString<ExternalCase>(data)
    }

    fun caseFile(caseName: String): File {
        return File("src/jvmTest/resources/cases/$caseName.json")
    }

    fun caseData(caseName: String): String {
        return FileUtils.readFileToString(caseFile(caseName), StandardCharsets.UTF_8)
    }

    fun createCase(caseName: String, glucoseValue: String = "0.667"): RDRCase {
        val builder1 = RDRCaseBuilder()
        builder1.addValue(glucose, defaultDate, glucoseValue)
        return builder1.build(caseName)
    }
}