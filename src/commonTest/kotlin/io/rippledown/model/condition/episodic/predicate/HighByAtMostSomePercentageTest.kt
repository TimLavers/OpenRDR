package io.rippledown.model.condition.episodic.predicate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import kotlin.test.Test

class HighByAtMostSomePercentageTest: Base() {
    private val fivePercentHigh = HighByAtMostSomePercentage(5)
    private val tenPercentHigh = HighByAtMostSomePercentage(10)

    @Test
    fun cutoffsForTenPercentHigh() {
        tenPercentHigh.evaluate(testResult(1.90, 1.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(1.11, 1.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(1.101, 1.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(1.10, 1.0)) shouldBe true
        tenPercentHigh.evaluate(testResult(1.05, 1.0)) shouldBe true
        tenPercentHigh.evaluate(testResult(1.00001, 1.0)) shouldBe true
        tenPercentHigh.evaluate(testResult(1.00000, 1.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(0.9999, 1.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(0.95, 1.0)) shouldBe false

        tenPercentHigh.evaluate(testResult(22.01, 20.0)) shouldBe false
        tenPercentHigh.evaluate(testResult(22.0, 20.0)) shouldBe true
        tenPercentHigh.evaluate(testResult(21.999, 20.0)) shouldBe true
    }

    @Test
    fun cutoffsForFivePercentHigh() {
        fivePercentHigh.evaluate(testResult(1.05, 1.0)) shouldBe true
        fivePercentHigh.evaluate(testResult(1.049, 1.0)) shouldBe true
        fivePercentHigh.evaluate(testResult(1.00001, 1.0)) shouldBe true
        fivePercentHigh.evaluate(testResult(1.00000, 1.0)) shouldBe false
        fivePercentHigh.evaluate(testResult(0.9999, 1.0)) shouldBe false
    }

    @Test
    fun valueHasNoReferenceRange() {
        tenPercentHigh.evaluate(TestResult("34")) shouldBe false
    }

    @Test
    fun rangeHasNoUpperBound() {
        val range = ReferenceRange("5.0", null)
        tenPercentHigh.evaluate(TestResult("12.0", range, "mmol/L")) shouldBe false
    }

    @Test
    fun valueNormal() {
        val range = ReferenceRange("5.0", "10.0")
        tenPercentHigh.evaluate(TestResult("8.0", range, null)) shouldBe false
    }

    @Test
    fun valueLow() {
        val range = ReferenceRange("5.0", "10.0")
        tenPercentHigh.evaluate(TestResult("4.0", range, null)) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        tenPercentHigh.evaluate(TestResult("whatever")) shouldBe false
    }

    @Test
    fun equality() {
        tenPercentHigh shouldBe HighByAtMostSomePercentage(10)
        tenPercentHigh shouldNotBe HighByAtMostSomePercentage(11)
    }

    @Test
    fun hash() {
        tenPercentHigh.hashCode() shouldBe HighByAtMostSomePercentage(10).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(fivePercentHigh) shouldBe fivePercentHigh
    }

    @Test
    fun description() {
        tenPercentHigh.description(false) shouldBe "is high by at most 10%"
        tenPercentHigh.description(true) shouldBe "are high by at most 10%"
    }

    private fun testResult(tshValue: Double, upperBound: Double): TestResult {
        val referenceRange = ReferenceRange("0.1", "$upperBound")
        return TestResult("$tshValue", referenceRange, "pmol/L")
    }

    private fun checkExceptionThrownForCutoff(cutoff: Int) {
        val exception = shouldThrow<IllegalArgumentException> {
            HighByAtMostSomePercentage(cutoff)
        }
        exception.message should startWith("Value should be an integer in the range [0, 100]")
    }
}