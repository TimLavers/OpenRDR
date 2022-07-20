package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RDRCaseTest {

    @Test
    fun getCaseData() {
        val case1 = RDRCase("Case1")
        assertTrue(case1.caseData.isEmpty())
    }

    @Test
    fun addValue() {
        val case1 = RDRCase("Case1")
        case1.addValue("TSH", "0.667")
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.caseData["TSH"], "0.667")
    }

    @Test
    fun addValueTwice() {
        val case1 = RDRCase("Case1")
        case1.addValue("TSH", "0.67")
        case1.addValue("TSH", "0.68")
        assertEquals(case1.caseData.size, 1)
        assertEquals(case1.caseData["TSH"], "0.68")
    }

    @Test
    fun getName() {
        val case1 = RDRCase("Case1")
        assertEquals(case1.name, "Case1")
    }

    @Test
    fun jsonSerialisation() {
        val case1 = RDRCase("Case1")
        val sd1 = serializeDeserialize(case1)
        assertEquals(sd1, case1)

        val case2 = RDRCase("Case2")
        case2.addValue("TSH", "0.667")
        case2.addValue("ABC", "6.7")
        val sd2 = serializeDeserialize(case2)
        assertEquals(sd2, case2)
    }

    private fun serializeDeserialize(rdrCase: RDRCase): RDRCase {
        val serialized = Json.encodeToString(rdrCase)
        return Json.decodeFromString(serialized)
    }
}