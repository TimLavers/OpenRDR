package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ResultTest {

    @Test
    fun construction() {
        val result = Result(Value("23"), ReferenceRange("20", "30"), "pmol/L")
        assertEquals(result.value, Value("23"))
        assertEquals(result.referenceRange, ReferenceRange("20", "30"))
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
    fun construction3() {
        val result = Result("23", ReferenceRange("20", "30"), "pmol/L")
        assertEquals(result.value, Value("23"))
        assertEquals(result.referenceRange, ReferenceRange("20", "30"))
        assertEquals(result.units, "pmol/L")
    }

    @Test
    fun construction4() {
        val result = Result("23", ReferenceRange("20", "30"))
        result.value shouldBe Value("23")
        result.referenceRange shouldBe ReferenceRange("20", "30")
        result.units shouldBe null
    }

    @Test
    fun jsonSerialisation() {
        val result1 = Result("1234")
        serializeDeserialize(result1)
    }

    @Test
    fun isHigh() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        Result("19", rr0, pm).isHigh() shouldBe false
        Result("20", rr0, pm).isHigh() shouldBe false
        Result("23", rr0, pm).isHigh() shouldBe false
        Result("29", rr0, pm).isHigh() shouldBe false
        Result("30", rr0, pm).isHigh() shouldBe false
        Result("31", rr0, pm).isHigh() shouldBe true
        Result("lots", rr0, pm).isHigh() shouldBe false

        val rr1 = ReferenceRange("20", null)
        Result("19", rr1, pm).isHigh() shouldBe false
        Result("20", rr1, pm).isHigh() shouldBe false
        Result("21", rr1, pm).isHigh() shouldBe false

        val rr2 = ReferenceRange(null, "20")
        Result("19", rr2, pm).isHigh() shouldBe false
        Result("20", rr2, pm).isHigh() shouldBe false
        Result("21", rr2, pm).isHigh() shouldBe true

        Result("19", null, pm).isHigh() shouldBe false
        Result("-20", null, pm).isHigh() shouldBe false
        Result("heaps", null, pm).isHigh() shouldBe false
    }

    @Test
    fun isNormal() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        Result("19", rr0, pm).isNormal() shouldBe false
        Result("20", rr0, pm).isNormal() shouldBe true
        Result("23", rr0, pm).isNormal() shouldBe true
        Result("29", rr0, pm).isNormal() shouldBe true
        Result("30", rr0, pm).isNormal() shouldBe true
        Result("31", rr0, pm).isNormal() shouldBe false
        Result("lots", rr0, pm).isNormal() shouldBe false

        val rr1 = ReferenceRange("20", null)
        Result("19", rr1, pm).isNormal() shouldBe false
        Result("20", rr1, pm).isNormal() shouldBe true
        Result("21", rr1, pm).isNormal() shouldBe true

        val rr2 = ReferenceRange(null, "20")
        Result("19", rr2, pm).isNormal() shouldBe true
        Result("20", rr2, pm).isNormal() shouldBe true
        Result("21", rr2, pm).isNormal() shouldBe false

        Result("19", null, pm).isNormal() shouldBe false
        Result("-20", null, pm).isNormal() shouldBe false
        Result("heaps", null, pm).isNormal() shouldBe false
    }

    @Test
    fun isLow() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        Result("-19099", rr0, pm).isLow() shouldBe true
        Result("19", rr0, pm).isLow() shouldBe true
        Result("20", rr0, pm).isLow() shouldBe false
        Result("23", rr0, pm).isLow() shouldBe false
        Result("29", rr0, pm).isLow() shouldBe false
        Result("30", rr0, pm).isLow() shouldBe false
        Result("31", rr0, pm).isLow() shouldBe false
        Result("nowt", rr0, pm).isLow() shouldBe false

        val rr1 = ReferenceRange("20", null)
        Result("19", rr1, pm).isLow() shouldBe true
        Result("20", rr1, pm).isLow() shouldBe false
        Result("21", rr1, pm).isLow() shouldBe false

        val rr2 = ReferenceRange(null, "20")
        Result("19", rr2, pm).isLow() shouldBe false
        Result("20", rr2, pm).isLow() shouldBe false
        Result("21", rr2, pm).isLow() shouldBe false

        Result("19", null, pm).isLow() shouldBe false
        Result("-20", null, pm).isLow() shouldBe false
        Result("heaps", null, pm).isLow() shouldBe false
    }

    private fun serializeDeserialize(result: Result): Result {
        val serialized = Json.encodeToString(result)
        return Json.decodeFromString(serialized)
    }
}