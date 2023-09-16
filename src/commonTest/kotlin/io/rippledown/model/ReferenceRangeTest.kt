package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

// ORD1
internal class ReferenceRangeTest {

    @Test
    fun validation() {
        assertFailsWith<IllegalStateException> {
            ReferenceRange(null, null)
        }
        assertFailsWith<IllegalStateException> {
            ReferenceRange("1.0", "1.0")
        }
        assertFailsWith<IllegalStateException> {
            ReferenceRange("1.0", "1.0")
        }
    }
    
    @Test
    fun isHigh() {
        val interval = ReferenceRange("2.0", "3.0")
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
    fun isHighNoUpperBound() {
        val interval = ReferenceRange("2.0", null)
        // Value float null
        assertFalse(interval.isHigh(Value("Whatever")))

        // Value float above upper bound
        assertFalse(interval.isHigh(Value("3.0001")))
        assertFalse(interval.isHigh(Value("35")))

        // Value float below lower bound
        assertFalse(interval.isHigh(Value("-15")))
        assertFalse(interval.isHigh(Value("1.99999")))

        // Value float is lower bound
        assertFalse(interval.isHigh(Value("2.0")))
    }

    @Test
    fun isHighNoLowerBound() {
        val interval = ReferenceRange(null, "3.0")
        // Value float null
        assertFalse(interval.isHigh(Value("Whatever")))

        // Value float above high cutoff
        assertTrue(interval.isHigh(Value("3.0001")))
        assertTrue(interval.isHigh(Value("3.5")))
        assertTrue(interval.isHigh(Value("35")))
        assertTrue(interval.isHigh(Value("350000")))

        // Value float below high cutoff
        assertFalse(interval.isHigh(Value("2.999")))
        assertFalse(interval.isHigh(Value("-1000")))

        // Value float is upper cutoff
        assertFalse(interval.isHigh(Value("3.0")))
    }

    @Test
    fun isNormal() {
        val interval = ReferenceRange("2.0", "3.0")
        // Value float null
        assertFalse(interval.isNormal(Value("Whatever")))
        
        // Value float above
        assertFalse(interval.isNormal(Value("3.0001")))
        assertFalse(interval.isNormal(Value("3.5")))
        assertFalse(interval.isNormal(Value("35")))
        assertFalse(interval.isNormal(Value("350000")))
        
        // Value float below
        assertFalse(interval.isNormal(Value("-15")))
        assertFalse(interval.isNormal(Value("-1.5")))
        assertFalse(interval.isNormal(Value("1.5")))
        assertFalse(interval.isNormal(Value("1.99999")))

        // Value float is lower value
        interval.isNormal(Value("2.0")) shouldBe true

        // Value float is upper value
        interval.isNormal(Value("3.0")) shouldBe true
        
        // Value float in range
        assertTrue(interval.isNormal(Value("2.00001")))
        assertTrue(interval.isNormal(Value("2.5")))
        assertTrue(interval.isNormal(Value("2.99999")))
    }

    @Test
    fun isNormalNoLowerBound() {
        val interval = ReferenceRange(null, "3.0")
        // Value float null
        assertFalse(interval.isNormal(Value("Whatever")))

        // Value float above
        assertFalse(interval.isNormal(Value("3.0001")))
        assertFalse(interval.isNormal(Value("350000")))

        // Value float below
        assertTrue(interval.isNormal(Value("2.99999")))
        assertTrue(interval.isNormal(Value("0.8")))

        // Value float is upper value
        interval.isNormal(Value("3.0")) shouldBe true
    }

    @Test
    fun isNormalNoUpperBound() {
        val interval = ReferenceRange("2.0", null)
        // Value float null
        assertFalse(interval.isNormal(Value("Whatever")))

        // Value float above
        assertTrue(interval.isNormal(Value("2.0001")))
        assertTrue(interval.isNormal(Value("20000")))

        // Value float below
        assertFalse(interval.isNormal(Value("-15")))
        assertFalse(interval.isNormal(Value("-1.5")))
        assertFalse(interval.isNormal(Value("1.5")))
        assertFalse(interval.isNormal(Value("1.99999")))

        // Value float is lower value
        interval.isNormal(Value("2.0")) shouldBe true
    }

    @Test
    fun isLow() {
        val interval = ReferenceRange("2.0", "3.0")
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
    fun isLowNoLowerBound() {
        val interval = ReferenceRange(null, "3.0")
        // Value float null
        assertFalse(interval.isLow(Value("Whatever")))

        // Value float above
        assertFalse(interval.isLow(Value("3.0000001")))
        assertFalse(interval.isLow(Value("3.5")))
        assertFalse(interval.isLow(Value("35")))
        assertFalse(interval.isLow(Value("350000")))

        // Value float below
        assertFalse(interval.isLow(Value("1.5")))
        assertFalse(interval.isLow(Value("2.99999")))

        // Value float is upper value
        assertFalse(interval.isLow(Value("3.0")))
    }

    @Test
    fun isLowNoUpperBound() {
        val interval = ReferenceRange("2.0", null)
        // Value float null
        assertFalse(interval.isLow(Value("Whatever")))

        // Value float above
        assertFalse(interval.isLow(Value("2.0000001")))
        assertFalse(interval.isLow(Value("35")))

        // Value float below
        assertTrue(interval.isLow(Value("-1.5")))
        assertTrue(interval.isLow(Value("1.5")))
        assertTrue(interval.isLow(Value("1.99999")))

        // Value float is lower value
        assertFalse(interval.isLow(Value("2.0")))
    }

    @Test
    fun jsonSerialisation() {
        val rr1 = ReferenceRange("0.5", "0.9")
        val sd1 = serializeDeserialize(rr1)
        assertEquals(sd1, rr1)

        val rr2 = ReferenceRange("0.5", null)
        val sd2 = serializeDeserialize(rr2)
        assertEquals(sd2, rr2)

        val rr3 = ReferenceRange(null, "34.0")
        val sd3 = serializeDeserialize(rr3)
        assertEquals(sd3, rr3)
    }

    private fun serializeDeserialize(interval: ReferenceRange): ReferenceRange {
        val serialized = Json.encodeToString(interval)
        return Json.decodeFromString(serialized)
    }
}