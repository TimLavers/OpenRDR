package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IsNotNumericTest: Base() {
    private val isNotNumeric = IsNotNumeric

    @Test
    fun valueBlank() {
        isNotNumeric.evaluate(TestResult("")) shouldBe true
    }

    @Test
    fun valueText() {
        isNotNumeric.evaluate(TestResult("whatever")) shouldBe true
        isNotNumeric.evaluate(TestResult("one")) shouldBe true
    }

    @Test
    fun valueNumeric() {
        isNotNumeric.evaluate(TestResult("-1")) shouldBe false
        isNotNumeric.evaluate(TestResult("-100")) shouldBe false
        isNotNumeric.evaluate(TestResult("0.00")) shouldBe false
        isNotNumeric.evaluate(TestResult("0.0007")) shouldBe false
        isNotNumeric.evaluate(TestResult("8.1322")) shouldBe false
    }

    @Test
    fun whitespace() {
        isNotNumeric.evaluate(TestResult(" ")) shouldBe true
    }

    @Test
    fun equalsTest() {
        isNotNumeric shouldNotBe IsNumeric
        isNotNumeric shouldBe IsNotNumeric
    }

    @Test
    fun hashCodeTest() {
        isNotNumeric.hashCode() shouldBe IsNotNumeric.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(isNotNumeric) shouldBe isNotNumeric
    }

    @Test
    fun description() {
        isNotNumeric.description(false) shouldBe "is not numeric"
        isNotNumeric.description(true) shouldBe "are not numeric"
    }
}