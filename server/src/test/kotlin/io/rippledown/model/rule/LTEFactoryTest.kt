package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import kotlin.test.Test

class LTEFactoryTest {
    @Test
    fun createFor() {
        LTEFactory(null).createFor() shouldBe null
        LTEFactory(TestResult("whatever")).createFor() shouldBe null
        LTEFactory(TestResult("1.0")).createFor() shouldBe LessThanOrEquals(1.0)
        LTEFactory(TestResult("10.01")).createFor() shouldBe LessThanOrEquals(10.01)
        LTEFactory(TestResult("10.01", ReferenceRange("5.0", "15.0"))).createFor() shouldBe LessThanOrEquals(10.01)
        LTEFactory(TestResult("10.01", ReferenceRange("5.0", "15.0"), "m/s")).createFor() shouldBe LessThanOrEquals(10.01)
    }
}