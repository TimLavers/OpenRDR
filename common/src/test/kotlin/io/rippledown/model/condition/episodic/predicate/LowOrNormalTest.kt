package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class LowOrNormalTest: Base() {
    private val lowOrNormal = LowOrNormal
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        lowOrNormal.evaluate(TestResult(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        lowOrNormal.evaluate(TestResult(Value(""), null, units)) shouldBe false
        lowOrNormal.evaluate(TestResult(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        lowOrNormal.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        lowOrNormal.evaluate(TestResult(Value("1.8"), range, units)) shouldBe true
        lowOrNormal.evaluate(TestResult(Value("1.0"), range, units)) shouldBe true
    }

    @Test
    fun valueLow() {
        lowOrNormal.evaluate(TestResult(Value("0.8"), range, units)) shouldBe true
    }

    @Test
    fun valueHigh() {
        lowOrNormal.evaluate(TestResult(Value("5.0"), range, units)) shouldBe false
    }

    @Test
    fun equalsTest() {
        lowOrNormal shouldBe LowOrNormal
    }

    @Test
    fun hashCodeTest() {
        lowOrNormal.hashCode() shouldBe LowOrNormal.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(lowOrNormal) shouldBe lowOrNormal
    }

    @Test
    fun description() {
        lowOrNormal.description(false) shouldBe "is low or normal"
        lowOrNormal.description(true) shouldBe "are low or normal"
    }
}