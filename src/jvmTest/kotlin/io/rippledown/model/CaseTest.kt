package io.rippledown.model

import io.rippledown.CaseTestUtils
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseTest {

    @Test
    fun readFromFile() {
        val caseString = CaseTestUtils.caseData("Case1")
        val format = Json { allowStructuredMapKeys = true }
        val deserialized = format.decodeFromString<RDRCase>(caseString)
        assertEquals(deserialized.caseData.size, 2)
        assertEquals(deserialized.name, "Case1")
        assertEquals(deserialized.get("TSH")!!.value.text, "0.667")
        assertEquals(deserialized.get("ABC")!!.value.text, "6.7")
    }
}