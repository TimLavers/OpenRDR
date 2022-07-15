package io.rippledown.model

import com.google.common.io.Files
import io.rippledown.CaseTestUtils
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

internal class CaseTest {

    @Test
    fun readFromFile() {
        val caseString = CaseTestUtils.caseData("Case1")
        val deserialized = Json.decodeFromString<RDRCase>(caseString)
        assertEquals(deserialized.caseData.size, 2)
        assertEquals(deserialized.name, "Case1")
        assertEquals(deserialized.caseData["TSH"], "0.667")
        assertEquals(deserialized.caseData["ABC"], "6.7")

    }
}