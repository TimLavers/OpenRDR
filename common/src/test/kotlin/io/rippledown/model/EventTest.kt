package io.rippledown.model

import io.rippledown.utils.defaultDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// ORD1
internal class EventTest {
    val tsh = Attribute(3, "TSH")

    @Test
    fun construction() {
        val te = Event(tsh, defaultDate)
        assertEquals(te.attribute, tsh)
        assertEquals(te.date, defaultDate)
    }

    @Test
    fun equalityRequiresSameDate() {
        val te1 = Event(tsh, defaultDate)
        val te2 = Event(tsh, defaultDate + 1234L)
        assertNotEquals(te1, te2)
    }

    @Test
    fun jsonSerialisation() {
        val te = Event(tsh, defaultDate)
        val sd = serializeDeserialize(te)
        assertEquals(sd, te)
    }

    private fun serializeDeserialize(Event: Event): Event {
        val serialized = Json.encodeToString(Event)
        return Json.decodeFromString(serialized)
    }
}