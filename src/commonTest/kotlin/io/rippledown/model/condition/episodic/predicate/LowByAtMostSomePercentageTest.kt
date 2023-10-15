package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import kotlin.test.Test

class LowByAtMostSomePercentageTest: Base() {
    private val tenPercentLow = LowByAtMostSomePercentage( 10)
    private val fivePercentLow = LowByAtMostSomePercentage( 5)

    @Test
    fun valueHasNoReferenceRange() {
        tenPercentLow.evaluate(TestResult("12")) shouldBe false
    }

    @Test
    fun rangeHasNoLowerBound() {
        val range = ReferenceRange(null, "5.0")
        tenPercentLow.evaluate(TestResult("0.4", range, null)) shouldBe false
    }

    @Test
    fun valueNormal() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(TestResult("2.4", range, null)) shouldBe false
    }

    @Test
    fun valueHigh() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(TestResult("5.4", range, null)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(TestResult("no result", range, null)) shouldBe false
    }

    @Test
    fun cutoffsForTenPercentLow() {
        tenPercentLow.evaluate(testResult(1.00001, 1.0)) shouldBe false
        tenPercentLow.evaluate(testResult(1.00000, 1.0)) shouldBe false
        tenPercentLow.evaluate(testResult(0.9999, 1.0)) shouldBe true
        tenPercentLow.evaluate(testResult(0.95, 1.0)) shouldBe true
        tenPercentLow.evaluate(testResult(0.90001, 1.0)) shouldBe true
        tenPercentLow.evaluate(testResult(0.90000, 1.0)) shouldBe true
        tenPercentLow.evaluate(testResult(0.89999, 1.0)) shouldBe true
        tenPercentLow.evaluate(testResult(0.899, 1.0)) shouldBe false
        tenPercentLow.evaluate(testResult(0.8, 1.0)) shouldBe false
        tenPercentLow.evaluate(testResult(0.0, 1.0)) shouldBe false
    }

    @Test
    fun cutoffsForFivePercentLow() {
        fivePercentLow.evaluate(testResult(1.00001, 1.0)) shouldBe false
        fivePercentLow.evaluate(testResult(1.00000, 1.0)) shouldBe false
        fivePercentLow.evaluate(testResult(0.9999, 1.0)) shouldBe true
        fivePercentLow.evaluate(testResult(0.95001, 1.0)) shouldBe true
        fivePercentLow.evaluate(testResult(0.95000, 1.0)) shouldBe true
        fivePercentLow.evaluate(testResult(0.94999, 1.0)) shouldBe true // Close enough
        fivePercentLow.evaluate(testResult(0.949, 1.0)) shouldBe false
        fivePercentLow.evaluate(testResult(0.94, 1.0)) shouldBe false
    }

    @Test
    fun equalsTest() {
        tenPercentLow shouldBe LowByAtMostSomePercentage(10)
        tenPercentLow shouldNotBe fivePercentLow
    }

    @Test
    fun hashCodeTest() {
        tenPercentLow.hashCode() shouldBe LowByAtMostSomePercentage(10).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(tenPercentLow) shouldBe tenPercentLow
    }

    @Test
    fun description() {
        fivePercentLow.description(false) shouldBe "is low by at most 5%"
        fivePercentLow.description(true) shouldBe "are low by at most 5%"
    }

    private fun testResult(tshValue: Double, lowerBound: Double): TestResult {
        val referenceRange = ReferenceRange("$lowerBound", "10.0")
        return TestResult("$tshValue", referenceRange, "pmol/L")
    }
}