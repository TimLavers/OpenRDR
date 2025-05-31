package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class GreaterThanTest : Base() {
    private val gt = GreaterThan(1.2)
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueNotNumeric() {
        gt.evaluate(TestResult(Value(""), range, units)) shouldBe false
        gt.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNumeric() {
        gt.evaluate(TestResult(Value("1.8"), range, units)) shouldBe true
        gt.evaluate(TestResult("1.8")) shouldBe true
        gt.evaluate(TestResult("1.2")) shouldBe false
        gt.evaluate(TestResult("1.19999")) shouldBe false
        gt.evaluate(TestResult("1.15")) shouldBe false
        gt.evaluate(TestResult("0")) shouldBe false
    }

    @Test
    fun equality() {
        GreaterThan(10.0) shouldBe GreaterThan(10.0)
        GreaterThan(10.0) shouldNotBe GreaterThan(1.0)
    }

    @Test
    fun hash() {
        GreaterThan(10.0).hashCode() shouldBe GreaterThan(10.0).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(gt) shouldBe gt
    }

    @Test
    fun description() {
        gt.description(false) shouldBe "> 1.2"
        gt.description(true) shouldBe "> 1.2"
    }
}