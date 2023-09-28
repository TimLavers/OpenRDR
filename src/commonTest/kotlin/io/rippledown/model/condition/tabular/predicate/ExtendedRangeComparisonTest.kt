package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.ReferenceRange
import io.rippledown.model.TestResult
import io.rippledown.model.Value
import kotlin.test.Test

class ExtendedRangeComparisonTest: Base() {
    private val erf0 = AllowNormalOrSlightlyHigh(10)

    @Test
    fun handleNonNumericalValue() {
        erf0.evaluate(TestResult("whatever")) shouldBe false
    }

    @Test
    fun handleNoRange() {
        erf0.evaluate(TestResult(Value("12"), null, "Furlongs")) shouldBe false
    }

    @Test
    fun rangeWithNoUpperLimit() {
        erf0.evaluate(TestResult(Value("12"), ReferenceRange("10", null), "Furlongs")) shouldBe false
    }

    @Test
    fun rangeWithNoLowerLimit() {
        erf0.evaluate(TestResult(Value("12"), ReferenceRange(null, "11"), "Furlongs")) shouldBe false
    }

    @Test
    fun aboveExtendedUpperLimit() {
        val nsh10 = AllowNormalOrSlightlyHigh(10)
        val rr5to10 = ReferenceRange("5", "10")
        nsh10.evaluate(TestResult("20", rr5to10, null)) shouldBe false
        nsh10.evaluate(TestResult("12", rr5to10, null)) shouldBe false
        nsh10.evaluate(TestResult("11.1", rr5to10, null)) shouldBe false
        nsh10.evaluate(TestResult("11.01", rr5to10, null)) shouldBe false
        nsh10.evaluate(TestResult("11.001", rr5to10, null)) shouldBe false
        nsh10.evaluate(TestResult("11.0001", rr5to10, null)) shouldBe false
        // Very close to the upper limit is true.
        nsh10.evaluate(TestResult("11.00001", rr5to10, null)) shouldBe true
        nsh10.evaluate(TestResult("11", rr5to10, null)) shouldBe true
    }

    @Test
    fun belowExtendedUpperLimitButAboveRangeLimit() {
        val rr20to100 = ReferenceRange("20", "100")
        with(AllowNormalOrSlightlyHigh(20)) {
            this.evaluate(TestResult("120", rr20to100)) shouldBe true
            this.evaluate(TestResult("119", rr20to100)) shouldBe true
            this.evaluate(TestResult("102", rr20to100)) shouldBe true
            this.evaluate(TestResult("100.0001", rr20to100)) shouldBe true
            this.evaluate(TestResult("100", rr20to100)) shouldBe true
        }

        with(AllowSlightlyHigh(20)) {
            this.evaluate(TestResult("120", rr20to100)) shouldBe true
            this.evaluate(TestResult("119", rr20to100)) shouldBe true
            this.evaluate(TestResult("102", rr20to100)) shouldBe true
            this.evaluate(TestResult("100.0001", rr20to100)) shouldBe true
            this.evaluate(TestResult("100.00001", rr20to100)) shouldBe true
            this.evaluate(TestResult("100.000001", rr20to100)) shouldBe false // counts as normal
            this.evaluate(TestResult("100", rr20to100)) shouldBe false
            this.evaluate(TestResult("99.999999", rr20to100)) shouldBe false
            this.evaluate(TestResult("99.99", rr20to100)) shouldBe false
            this.evaluate(TestResult("99.9", rr20to100)) shouldBe false
            this.evaluate(TestResult("99", rr20to100)) shouldBe false
        }
    }

    @Test
    fun allowNormalsOrSlightlyHigh(){
        AllowNormalOrSlightlyHigh(20).includeNormalResults() shouldBe true
    }

    @Test
    fun allowSlightlyHigh(){
        AllowSlightlyHigh(20).includeNormalResults() shouldBe false
    }
}