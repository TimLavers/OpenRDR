package io.rippledown

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.defaultDate
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

internal object CaseTestUtils {

    private val glucose = Attribute(-200, "Glucose")

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