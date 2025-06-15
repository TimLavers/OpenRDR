package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class LessThanTest : Base() {
    private val lt = LessThan(1.5)
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueNotNumeric() {
        lt.evaluate(TestResult(Value(""), range, units)) shouldBe false
        lt.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNumeric() {
        lt.evaluate(TestResult(Value("0.8"), range, units)) shouldBe true
        lt.evaluate(TestResult("1.8")) shouldBe false
        lt.evaluate(TestResult("1.5")) shouldBe false
        lt.evaluate(TestResult("1.5000000000")) shouldBe false
        lt.evaluate(TestResult("1.4999999999")) shouldBe true
        lt.evaluate(TestResult("1.500001")) shouldBe false
        lt.evaluate(TestResult("0")) shouldBe true
    }

    @Test
    fun equality() {
        LessThan(10.0) shouldBe LessThan(10.0)
        LessThan(10.0) shouldNotBe LessThan(1.0)
    }

    @Test
    fun hash() {
        LessThan(10.0).hashCode() shouldBe LessThan(10.0).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(lt) shouldBe lt
    }

    @Test
    fun description() {
        lt.description(false) shouldBe "< 1.5"
        lt.description(true) shouldBe "< 1.5"
    }
}