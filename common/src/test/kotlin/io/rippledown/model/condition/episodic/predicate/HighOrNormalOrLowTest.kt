package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class HighOrNormalOrLowTest: Base() {
    private val hasRange = HighOrNormalOrLow
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        hasRange.evaluate(TestResult(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        hasRange.evaluate(TestResult(Value(""), null, units)) shouldBe false
        hasRange.evaluate(TestResult(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        hasRange.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        hasRange.evaluate(TestResult(Value("1.8"), range, units)) shouldBe true
        hasRange.evaluate(TestResult(Value("1.0"), range, units)) shouldBe true
    }

    @Test
    fun valueLow() {
        hasRange.evaluate(TestResult(Value("0.8"), range, units)) shouldBe true
    }

    @Test
    fun valueHigh() {
        hasRange.evaluate(TestResult(Value("5.0"), range, units)) shouldBe true
    }

    @Test
    fun equalsTest() {
        hasRange shouldNotBe HighOrNormal
        hasRange shouldBe HighOrNormalOrLow
    }

    @Test
    fun hashCodeTest() {
        hasRange.hashCode() shouldBe HighOrNormalOrLow.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(hasRange) shouldBe hasRange
    }

    @Test
    fun description() {
        hasRange.description(false) shouldBe "is high or normal or low"
        hasRange.description(true) shouldBe "are high or normal or low"
    }
}