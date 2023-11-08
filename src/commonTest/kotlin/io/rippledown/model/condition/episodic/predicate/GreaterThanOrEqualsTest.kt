package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import kotlin.test.Test

class GreaterThanOrEqualsTest : Base() {
    private val gte = GreaterThanOrEquals(1.2)
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueNotNumeric() {
        gte.evaluate(TestResult(Value(""), range, units)) shouldBe false
        gte.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNumeric() {
        gte.evaluate(TestResult(Value("1.8"), range, units)) shouldBe true
        gte.evaluate(TestResult("1.8")) shouldBe true
        gte.evaluate(TestResult("1.2")) shouldBe true
        gte.evaluate(TestResult("1.19999")) shouldBe false
        gte.evaluate(TestResult("1.15")) shouldBe false
        gte.evaluate(TestResult("0")) shouldBe false
    }

    @Test
    fun equality() {
        GreaterThanOrEquals(10.0) shouldBe GreaterThanOrEquals(10.0)
        GreaterThanOrEquals(10.0) shouldNotBe GreaterThanOrEquals(1.0)
    }

    @Test
    fun hash() {
        GreaterThanOrEquals(10.0).hashCode() shouldBe GreaterThanOrEquals(10.0).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(gte) shouldBe gte
    }

    @Test
    fun description() {
        gte.description(false) shouldBe " ≥ 1.2"
        gte.description(true) shouldBe " ≥ 1.2"
    }
}