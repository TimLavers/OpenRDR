package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

const val defaultTestDate = 1659752689505L

// ORD1
internal class TestEventTest {
    val tsh = Attribute("TSH", 3)

    @Test
    fun construction() {
        val te = TestEvent(tsh, defaultTestDate)
        assertEquals(te.attribute, tsh)
        assertEquals(te.date, defaultTestDate)
    }

    @Test
    fun equalityRequiresSameDate() {
        val te1 = TestEvent(tsh, defaultTestDate)
        val te2 = TestEvent(tsh, defaultTestDate + 1234L)
        assertNotEquals(te1, te2)
    }

    @Test
    fun jsonSerialisation() {
        val te = TestEvent(tsh, defaultTestDate)
        val sd = serializeDeserialize(te)
        assertEquals(sd, te)
    }

    private fun serializeDeserialize(testEvent: TestEvent):TestEvent {
        val serialized = Json.encodeToString(testEvent)
        return Json.decodeFromString(serialized)
    }
}