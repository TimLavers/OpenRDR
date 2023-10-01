package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import kotlin.test.Test

class NormalOrLowByAtMostSomePercentageTest: Base() {
    private val tenPercentLow = NormalOrLowByAtMostSomePercentage( 10)
    private val fivePercentLow = NormalOrLowByAtMostSomePercentage( 5)

    @Test
    fun valueNormal() {
        val range = ReferenceRange("1.0", "5.0")
        tenPercentLow.evaluate(TestResult("2.4", range, null)) shouldBe true
    }

    @Test
    fun equalsTest() {
        tenPercentLow shouldBe NormalOrLowByAtMostSomePercentage(10)
        tenPercentLow shouldNotBe fivePercentLow
    }

    @Test
    fun hashCodeTest() {
        tenPercentLow.hashCode() shouldBe NormalOrLowByAtMostSomePercentage(10).hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(tenPercentLow) shouldBe tenPercentLow
    }

    @Test
    fun description() {
        fivePercentLow.description(false) shouldBe "is normal or low by at most 5%"
        fivePercentLow.description(true) shouldBe "are normal or low by at most 5%"
    }
}