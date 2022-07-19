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
        val deserialized = Json.decodeFromString<RDRCase>(caseString)
        assertEquals(deserialized.caseData.size, 2)
        assertEquals(deserialized.name, "Case1")
        assertEquals(deserialized.caseData["TSH"], "0.667")
        assertEquals(deserialized.caseData["ABC"], "6.7")
    }
}