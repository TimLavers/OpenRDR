package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class HighTest : Base() {
    private val high = High
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        high.evaluate(Result(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        high.evaluate(Result(Value(""), null, units)) shouldBe false
        high.evaluate(Result(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        high.evaluate(Result(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        high.evaluate(Result(Value("1.8"), range, units)) shouldBe false
        high.evaluate(Result(Value("1.0"), range, units)) shouldBe false
    }

    @Test
    fun valueLow() {
        high.evaluate(Result(Value("0.8"), range, units)) shouldBe false
    }

    @Test
    fun valueHigh() {
        high.evaluate(Result(Value("5.0"), range, units)) shouldBe true
    }

    @Test
    fun serialization() {
        serializeDeserialize(high) shouldBe high
    }

    @Test
    fun description() {
        high.description(false) shouldBe "is high"
        high.description(true) shouldBe "are high"
    }
}