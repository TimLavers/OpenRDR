package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.Result
import io.rippledown.model.Value
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class NormalTest: Base() {
    private val normal = Normal
    private val range = ReferenceRange("1.0", "2.5")
    private val units = "g"

    @Test
    fun valueBlank() {
        normal.evaluate(Result(Value(""), range, units)) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        normal.evaluate(Result(Value(""), null, units)) shouldBe false
        normal.evaluate(Result(Value("1.2"), null, units)) shouldBe false
    }

    @Test
    fun valueNotNumeric() {
        normal.evaluate(Result(Value("whatever"), range, units)) shouldBe false
    }

    @Test
    fun valueNormal() {
        normal.evaluate(Result(Value("1.8"), range, units)) shouldBe true
        normal.evaluate(Result(Value("1.0"), range, units)) shouldBe true
    }

    @Test
    fun valueLow() {
        normal.evaluate(Result(Value("0.8"), range, units)) shouldBe false
    }

    @Test
    fun valueHigh() {
        normal.evaluate(Result(Value("5.0"), range, units)) shouldBe false
    }

    @Test
    fun serialization() {
        serializeDeserialize(normal) shouldBe normal
    }

    @Test
    fun description() {
        normal.description(false) shouldBe "is normal"
        normal.description(true) shouldBe "are normal"
    }
}