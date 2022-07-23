package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TestResultTest {

    @Test
    fun construction() {
        val result = TestResult(Value("23"), ReferenceRange("20", "30"), "pmol/L")
        assertEquals(result.value, Value("23"))
        assertEquals(result.referenceRange, ReferenceRange("20", "30"))
        assertEquals(result.units, "pmol/L")
    }

    @Test
    fun construction2() {
        val result = TestResult("1234")
        assertEquals(result.value, Value("1234"))
        assertNull(result.units)
        assertNull(result.referenceRange)
    }

    @Test
    fun jsonSerialisation() {
        val result1 = TestResult("1234")
        val sd1 = serializeDeserialize(result1)
        assertEquals(sd1, result1)

        val result2 = TestResult(Value("23"), ReferenceRange("20", "30"), "pmol/L")
        val sd2 = serializeDeserialize(result2)
        assertEquals(sd2, result2)
    }

    private fun serializeDeserialize(result: TestResult): TestResult {
        val serialized = Json.encodeToString(result)
        return Json.decodeFromString(serialized)
    }
}