package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ResultTest {

    @Test
    fun construction() {
        val result = Result(Value("23"), ReferenceRange(20.0F, 30.0F), "pmol/L")
        assertEquals(result.value, Value("23"))
        assertEquals(result.referenceRange, ReferenceRange(20.0F, 30.0F))
        assertEquals(result.units, "pmol/L")
    }

    @Test
    fun construction2() {
        val result = Result("1234")
        assertEquals(result.value, Value("1234"))
        assertNull(result.units)
        assertNull(result.referenceRange)
    }

    @Test
    fun jsonSerialisation() {
        val result1 = Result("1234")
        val sd1 = serializeDeserialize(result1)
        assertEquals(sd1, result1)

        val result2 = Result(Value("23"), ReferenceRange(20.0F, 30.0F), "pmol/L")
        val sd2 = serializeDeserialize(result2)
        assertEquals(sd2, result2)
    }

    private fun serializeDeserialize(result: Result): Result {
        val serialized = Json.encodeToString(result)
        return Json.decodeFromString(serialized)
    }
}