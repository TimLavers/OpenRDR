package io.rippledown.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// ORD1
internal class ValueTest {

    @Test
    fun construction() {
        val value = Value("blah")
        assertEquals(value.text, "blah")
        assertNull(value.real)
    }

    @Test
    fun real() {
        val value = Value("2.718")
        assertEquals(value.text, "2.718")
        assertEquals(value.real!!, 2.718)
    }

    @Test
    fun jsonSerialisation() {
        val tsh = Value("Whatever")
        val sd = serializeDeserialize(tsh)
        assertEquals(sd, tsh)
    }

    private fun serializeDeserialize(value: Value): Value {
        val serialized = Json.encodeToString(value)
        return Json.decodeFromString(serialized)
    }
}