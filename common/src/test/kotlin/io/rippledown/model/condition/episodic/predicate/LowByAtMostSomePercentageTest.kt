package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class LowByAtMostSomePercentageTest: Base() {
    private val tenPercentLow = LowByAtMostSomePercentage( 10)
    private val fivePercentLow = LowByAtMostSomePercentage( 5)

    @Test
    fun valueHasNoReferenceRange() {
        tenPercentLow.evaluate(Result("12")) shouldBe false
    }

    @Test
    fun rangeHasNoLowerBound() {
        val range = ReferenceRange(null, "5.0")
        tenPercentLow.evaluate(Result("0.4", range, null)) shouldBe false
    }

    @Test
    fun valueNormal() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(Result("2.4", range, null)) shouldBe false
    }

    @Test
    fun valueHigh() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(Result("5.4", range, null)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(Result("no result", range, null)) shouldBe false
    }

    @Test
    fun cutoffsForTenPercentLow() {
        tenPercentLow.evaluate(Result(1.00001, 1.0)) shouldBe false
        tenPercentLow.evaluate(Result(1.00000, 1.0)) shouldBe false
        tenPercentLow.evaluate(Result(0.9999, 1.0)) shouldBe true
        tenPercentLow.evaluate(Result(0.95, 1.0)) shouldBe true
        tenPercentLow.evaluate(Result(0.90001, 1.0)) shouldBe true
        tenPercentLow.evaluate(Result(0.90000, 1.0)) shouldBe true
        tenPercentLow.evaluate(Result(0.89999, 1.0)) shouldBe true
        tenPercentLow.evaluate(Result(0.899, 1.0)) shouldBe false
        tenPercentLow.evaluate(Result(0.8, 1.0)) shouldBe false
        tenPercentLow.evaluate(Result(0.0, 1.0)) shouldBe false
    }

    @Test
    fun cutoffsForFivePercentLow() {
        fivePercentLow.evaluate(Result(1.00001, 1.0)) shouldBe false
        fivePercentLow.evaluate(Result(1.00000, 1.0)) shouldBe false
        fivePercentLow.evaluate(Result(0.9999, 1.0)) shouldBe true
        fivePercentLow.evaluate(Result(0.95001, 1.0)) shouldBe true
        fivePercentLow.evaluate(Result(0.95000, 1.0)) shouldBe true
        fivePercentLow.evaluate(Result(0.94999, 1.0)) shouldBe true // Close enough
        fivePercentLow.evaluate(Result(0.949, 1.0)) shouldBe false
        fivePercentLow.evaluate(Result(0.94, 1.0)) shouldBe false
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

    private fun Result(tshValue: Double, lowerBound: Double): Result {
        val referenceRange = ReferenceRange("$lowerBound", "10.0")
        return Result("$tshValue", referenceRange, "pmol/L")
    }
}