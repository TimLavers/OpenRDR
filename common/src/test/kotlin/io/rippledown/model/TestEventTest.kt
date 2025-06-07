package io.rippledown.model

import io.rippledown.utils.defaultDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// ORD1
internal class TestEventTest {
    val tsh = Attribute(3, "TSH")

    @Test
    fun construction() {
        val te = TestEvent(tsh, defaultDate)
        assertEquals(te.attribute, tsh)
        assertEquals(te.date, defaultDate)
    }

    @Test
    fun equalityRequiresSameDate() {
        val te1 = TestEvent(tsh, defaultDate)
        val te2 = TestEvent(tsh, defaultDate + 1234L)
        assertNotEquals(te1, te2)
    }

    @Test
    fun jsonSerialisation() {
        val te = TestEvent(tsh, defaultDate)
        val sd = serializeDeserialize(te)
        assertEquals(sd, te)
    }

    private fun serializeDeserialize(testEvent: TestEvent):TestEvent {
        val serialized = Json.encodeToString(testEvent)
        return Json.decodeFromString(serialized)
    }
}