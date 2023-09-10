package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.*
import kotlin.test.Test

class LessThanOrEqualsTest: Base() {
    private val lte = LessThanOrEquals(1.5)
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueNotNumeric() {
        lte.evaluate(TestResult(Value(""), range, units)) shouldBe false
        lte.evaluate(TestResult(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNumeric() {
        lte.evaluate(TestResult(Value("0.8"), range, units)) shouldBe true
        lte.evaluate(TestResult("1.8")) shouldBe false
        lte.evaluate(TestResult("1.5")) shouldBe true
        lte.evaluate(TestResult("1.49999")) shouldBe true
        lte.evaluate(TestResult("1.500001")) shouldBe false
        lte.evaluate(TestResult("0")) shouldBe true
    }

    @Test
    fun equality() {
        LessThanOrEquals(10.0) shouldBe LessThanOrEquals(10.0)
        LessThanOrEquals(10.0) shouldNotBe LessThanOrEquals(1.0)
    }

    @Test
    fun hash() {
        LessThanOrEquals(10.0).hashCode() shouldBe LessThanOrEquals(10.0).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(lte) shouldBe lte
    }

    @Test
    fun description() {
        lte.description(false) shouldBe " ≤ 1.5"
        lte.description(true) shouldBe " ≤ 1.5"
    }
}