package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IsNumericTest: Base() {
    private val isNumeric = IsNumeric

    @Test
    fun valueBlank() {
        isNumeric.evaluate(Result("")) shouldBe false
    }

    @Test
    fun valueText() {
        isNumeric.evaluate(Result("whatever")) shouldBe false
        isNumeric.evaluate(Result("one")) shouldBe false
    }

    @Test
    fun valueNumeric() {
        isNumeric.evaluate(Result("-1")) shouldBe true
        isNumeric.evaluate(Result("-100")) shouldBe true
        isNumeric.evaluate(Result("0.00")) shouldBe true
        isNumeric.evaluate(Result("0.0007")) shouldBe true
        isNumeric.evaluate(Result("8.1322")) shouldBe true
    }

    @Test
    fun whitespace() {
        isNumeric.evaluate(Result(" ")) shouldBe false
    }

    @Test
    fun equalsTest() {
        isNumeric shouldBe IsNumeric
    }

    @Test
    fun hashCodeTest() {
        isNumeric.hashCode() shouldBe IsNumeric.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(isNumeric) shouldBe isNumeric
    }

    @Test
    fun description() {
        isNumeric.description(false) shouldBe "is numeric"
        isNumeric.description(true) shouldBe "are numeric"
    }
}