package io.rippledown.model

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
}