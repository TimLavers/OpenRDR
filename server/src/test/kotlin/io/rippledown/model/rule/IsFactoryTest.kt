package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.Is
import kotlin.test.Test

class IsFactoryTest {
    @Test
    fun createFor() {
        IsFactory(null).createFor() shouldBe null
        IsFactory(TestResult("whatever")).createFor() shouldBe Is("whatever")
        IsFactory(TestResult("1.0")).createFor() shouldBe Is("1.0")
        IsFactory(TestResult("10.01", ReferenceRange("5.0", "15.0"), "m/s")).createFor() shouldBe Is("10.01")
    }
}