package io.rippledown.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

// ORD1
internal class ResultsListTest {

    @Test
    fun construction() {
        val tsh0 = TestResult(Value("6.5"), ReferenceRange("3.0", "5.9"), "kg/L")
        val tsh1 = TestResult(Value("6.6"), ReferenceRange("3.0", "5.9"), "kg/L")
        val tsh2 = TestResult(Value("6.7"), ReferenceRange("3.0", "5.9"), "kg/L")
        val resultsList = ResultsList(listOf(tsh0, tsh1, tsh2))
        assertEquals(3, resultsList.size)
        assertEquals(tsh0, resultsList[0])
        assertEquals(tsh1, resultsList[1])
        assertEquals(tsh2, resultsList[2])
        assertEquals("kg/L", resultsList.units)
    }

    @Test
    fun emptyListProhibited() {
        assertFailsWith<Exception> {
            ResultsList(emptyList())
        }
    }

    @Test
    fun differentUnitsProhibited() {
        val tsh0 = TestResult(Value("6.5"), ReferenceRange("3.0", "5.9"), "g/L")
        val tsh1 = TestResult(Value("6.6"), ReferenceRange("3.0", "5.9"), "kg/L")
        assertFailsWith<Exception> {
            ResultsList(listOf(tsh0, tsh1))
        }
    }

    @Test
    fun nullUnitsOK() {
        val tsh0 = TestResult(Value("6.5"), ReferenceRange("3.0", "5.9"), null)
        val tsh1 = TestResult(Value("6.6"), ReferenceRange("3.0", "5.9"), null)
        val tsh2 = TestResult(Value("6.7"), ReferenceRange("3.0", "5.9"), null)
        val resultsList = ResultsList(listOf(tsh0, tsh1, tsh2))
        assertNull(resultsList.units)
    }
}