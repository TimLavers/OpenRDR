package io.rippledown

import com.google.common.io.Files
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.io.FileUtils
import org.apache.commons.io.StandardLineSeparator
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object CaseTestUtils {
    val casesDir = File("src/jvmTest/resources/cases")

    fun caseFile(caseName: String): File {
        return File("src/jvmTest/resources/cases/$caseName.json")
    }

    fun caseData(caseName: String): String {
        return FileUtils.readFileToString(caseFile(caseName), StandardCharsets.UTF_8)
    }
}