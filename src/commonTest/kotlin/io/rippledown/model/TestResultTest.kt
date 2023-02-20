package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.kotest.matchers.shouldBe

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
    fun construction3() {
        val result = TestResult("23", ReferenceRange("20", "30"), "pmol/L")
        assertEquals(result.value, Value("23"))
        assertEquals(result.referenceRange, ReferenceRange("20", "30"))
        assertEquals(result.units, "pmol/L")
    }

    @Test
    fun jsonSerialisation() {
        val result1 = TestResult("1234")
        serializeDeserialize(result1)
    }

    @Test
    fun isHigh() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        TestResult("19", rr0, pm).isHigh() shouldBe false
        TestResult("20", rr0, pm).isHigh() shouldBe false
        TestResult("23", rr0, pm).isHigh() shouldBe false
        TestResult("29", rr0, pm).isHigh() shouldBe false
        TestResult("30", rr0, pm).isHigh() shouldBe false
        TestResult("31", rr0, pm).isHigh() shouldBe true
        TestResult("lots", rr0, pm).isHigh() shouldBe false

        val rr1 = ReferenceRange("20", null)
        TestResult("19", rr1, pm).isHigh() shouldBe false
        TestResult("20", rr1, pm).isHigh() shouldBe false
        TestResult("21", rr1, pm).isHigh() shouldBe false

        val rr2 = ReferenceRange(null, "20")
        TestResult("19", rr2, pm).isHigh() shouldBe false
        TestResult("20", rr2, pm).isHigh() shouldBe false
        TestResult("21", rr2, pm).isHigh() shouldBe true

        TestResult("19", null, pm).isHigh() shouldBe false
        TestResult("-20", null, pm).isHigh() shouldBe false
        TestResult("heaps", null, pm).isHigh() shouldBe false
    }

    @Test
    fun isNormal() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        TestResult("19", rr0, pm).isNormal() shouldBe false
        TestResult("20", rr0, pm).isNormal() shouldBe true
        TestResult("23", rr0, pm).isNormal() shouldBe true
        TestResult("29", rr0, pm).isNormal() shouldBe true
        TestResult("30", rr0, pm).isNormal() shouldBe true
        TestResult("31", rr0, pm).isNormal() shouldBe false
        TestResult("lots", rr0, pm).isNormal() shouldBe false

        val rr1 = ReferenceRange("20", null)
        TestResult("19", rr1, pm).isNormal() shouldBe false
        TestResult("20", rr1, pm).isNormal() shouldBe true
        TestResult("21", rr1, pm).isNormal() shouldBe true

        val rr2 = ReferenceRange(null, "20")
        TestResult("19", rr2, pm).isNormal() shouldBe true
        TestResult("20", rr2, pm).isNormal() shouldBe true
        TestResult("21", rr2, pm).isNormal() shouldBe false

        TestResult("19", null, pm).isNormal() shouldBe false
        TestResult("-20", null, pm).isNormal() shouldBe false
        TestResult("heaps", null, pm).isNormal() shouldBe false
    }
    
    @Test
    fun isLow() {
        val rr0 = ReferenceRange("20", "30")
        val pm = "pmol/L"
        TestResult("-19099", rr0, pm).isLow() shouldBe true
        TestResult("19", rr0, pm).isLow() shouldBe true
        TestResult("20", rr0, pm).isLow() shouldBe false
        TestResult("23", rr0, pm).isLow() shouldBe false
        TestResult("29", rr0, pm).isLow() shouldBe false
        TestResult("30", rr0, pm).isLow() shouldBe false
        TestResult("31", rr0, pm).isLow() shouldBe false
        TestResult("nowt", rr0, pm).isLow() shouldBe false

        val rr1 = ReferenceRange("20", null)
        TestResult("19", rr1, pm).isLow() shouldBe true
        TestResult("20", rr1, pm).isLow() shouldBe false
        TestResult("21", rr1, pm).isLow() shouldBe false

        val rr2 = ReferenceRange(null, "20")
        TestResult("19", rr2, pm).isLow() shouldBe false
        TestResult("20", rr2, pm).isLow() shouldBe false
        TestResult("21", rr2, pm).isLow() shouldBe false

        TestResult("19", null, pm).isLow() shouldBe false
        TestResult("-20", null, pm).isLow() shouldBe false
        TestResult("heaps", null, pm).isLow() shouldBe false
    }

    private fun serializeDeserialize(result: TestResult): TestResult {
        val serialized = Json.encodeToString(result)
        return Json.decodeFromString(serialized)
    }
}