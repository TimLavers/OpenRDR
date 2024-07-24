package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import kotlin.test.Test

class GTEFactoryTest {
    @Test
    fun createFor() {
        GTEFactory(null).createFor() shouldBe null
        GTEFactory(TestResult("whatever")).createFor() shouldBe null
        GTEFactory(TestResult("1.0")).createFor() shouldBe GreaterThanOrEquals(1.0)
        GTEFactory(TestResult("10.01")).createFor() shouldBe GreaterThanOrEquals(10.01)
        GTEFactory(TestResult("10.01", ReferenceRange("5.0", "15.0"))).createFor() shouldBe GreaterThanOrEquals(10.01)
        GTEFactory(TestResult("10.01", ReferenceRange("5.0", "15.0"), "m/s")).createFor() shouldBe GreaterThanOrEquals(10.01)
    }
}