package io.rippledown

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

internal object CaseTestUtils {

    fun caseFile(caseName: String): File {
        return File("src/jvmTest/resources/cases/$caseName.json")
    }

    fun caseData(caseName: String): String {
        return FileUtils.readFileToString(caseFile(caseName), StandardCharsets.UTF_8)
    }
}