package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

// ORD1
internal class ClosedClosedTest {

    @Test
    fun validation() {
        assertFailsWith<IllegalStateException> {
            ClosedClosed(1.0F, 0.5F)
        }
        assertFailsWith<IllegalStateException> {
            ClosedClosed(1.0F, 1.0F)
        }
    }
    
    @Test
    fun isHigh() {
        val interval = ClosedClosed(2.0F, 3.0F)
        // Value float null
        assertFalse(interval.isHigh(Value("Whatever")))
        
        // Value float above
        assertTrue(interval.isHigh(Value("3.0001")))
        assertTrue(interval.isHigh(Value("3.5")))
        assertTrue(interval.isHigh(Value("35")))
        assertTrue(interval.isHigh(Value("350000")))
        
        // Value float below
        assertFalse(interval.isHigh(Value("-15")))
        assertFalse(interval.isHigh(Value("-1.5")))
        assertFalse(interval.isHigh(Value("1.5")))
        assertFalse(interval.isHigh(Value("1.99999")))

        // Value float is lower value
        assertFalse(interval.isHigh(Value("2.0")))

        // Value float is upper value
        assertFalse(interval.isHigh(Value("3.0")))
        
        // Value float in range
        assertFalse(interval.isHigh(Value("2.5")))
    }

    @Test
    fun isNormal() {
        val interval = ClosedClosed(2.0F, 3.0F)
        // Value float null
        assertFalse(interval.isNormal(Value("Whatever")))
        
        // Value float above
        assertFalse(interval.isNormal(Value("3.0000001")))
        assertFalse(interval.isNormal(Value("3.5")))
        assertFalse(interval.isNormal(Value("35")))
        assertFalse(interval.isNormal(Value("350000")))
        
        // Value float below
        assertFalse(interval.isNormal(Value("-15")))
        assertFalse(interval.isNormal(Value("-1.5")))
        assertFalse(interval.isNormal(Value("1.5")))
        assertFalse(interval.isNormal(Value("1.99999")))

        // Value float is lower value
        assertFalse(interval.isNormal(Value("2.0")))

        // Value float is upper value
        assertFalse(interval.isNormal(Value("3.0")))
        
        // Value float in range
        assertTrue(interval.isNormal(Value("2.00001")))
        assertTrue(interval.isNormal(Value("2.5")))
        assertTrue(interval.isNormal(Value("2.99999")))
    }

    @Test
    fun isLow() {
        val interval = ClosedClosed(2.0F, 3.0F)
        // Value float null
        assertFalse(interval.isLow(Value("Whatever")))
        
        // Value float above
        assertFalse(interval.isLow(Value("3.0000001")))
        assertFalse(interval.isLow(Value("3.5")))
        assertFalse(interval.isLow(Value("35")))
        assertFalse(interval.isLow(Value("350000")))
        
        // Value float below
        assertTrue(interval.isLow(Value("-15")))
        assertTrue(interval.isLow(Value("-1.5")))
        assertTrue(interval.isLow(Value("1.5")))
        assertTrue(interval.isLow(Value("1.99999")))

        // Value float is lower value
        assertFalse(interval.isLow(Value("2.0")))

        // Value float is upper value
        assertFalse(interval.isLow(Value("3.0")))
        
        // Value float in range
        assertFalse(interval.isLow(Value("2.00001")))
        assertFalse(interval.isLow(Value("2.5")))
        assertFalse(interval.isLow(Value("2.99999")))
    }

    @Test
    fun jsonSerialisation() {
        val tsh = ClosedClosed(0.5F, 0.9F)
        val sd = serializeDeserialize(tsh)
        assertEquals(sd, tsh)
    }

    private fun serializeDeserialize(interval: ClosedClosed): ClosedClosed {
        val serialized = Json.encodeToString(interval)
        return Json.decodeFromString(serialized)
    }
}