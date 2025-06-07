package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class HighOrNormalTest: Base() {
    private val highOrNormal = HighOrNormal
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        highOrNormal.evaluate(TestResult(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        highOrNormal.evaluate(TestResult(Value(""), null, units)) shouldBe false
        highOrNormal.evaluate(TestResult(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        highOrNormal.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        highOrNormal.evaluate(TestResult(Value("1.8"), range, units)) shouldBe true
        highOrNormal.evaluate(TestResult(Value("1.0"), range, units)) shouldBe true
    }

    @Test
    fun valueLow() {
        highOrNormal.evaluate(TestResult(Value("0.8"), range, units)) shouldBe false
    }

    @Test
    fun valueHigh() {
        highOrNormal.evaluate(TestResult(Value("5.0"), range, units)) shouldBe true
    }

    @Test
    fun equalsTest() {
        highOrNormal shouldBe HighOrNormal
    }

    @Test
    fun hashCodeTest() {
        highOrNormal.hashCode() shouldBe HighOrNormal.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(highOrNormal) shouldBe highOrNormal
    }

    @Test
    fun description() {
        highOrNormal.description(false) shouldBe "is high or normal"
        highOrNormal.description(true) shouldBe "are high or normal"
    }
}