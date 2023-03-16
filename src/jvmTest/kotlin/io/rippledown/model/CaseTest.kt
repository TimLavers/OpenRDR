package io.rippledown.model

import io.rippledown.CaseTestUtils
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseTest {

    @Test
    fun readFromFile() {
        val caseString = CaseTestUtils.caseData("Case1")
        val format = Json { allowStructuredMapKeys = true }
        val deserialized = format.decodeFromString<RDRCase>(caseString)
        assertEquals(2, deserialized.data.size)
        assertEquals(deserialized.name, "Case1")
        assertEquals(deserialized.getLatest(Attribute("TSH", 1))!!.value.text, "0.667")
        assertEquals(deserialized.getLatest(Attribute("ABC", 2))!!.value.text, "6.7")
    }
}