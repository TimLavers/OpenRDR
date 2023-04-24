package io.rippledown.model.condition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.startWith
import io.rippledown.model.*
import kotlin.test.Test

internal class SlightlyLowTest: ConditionTestBase() {

    private val tenPercentLow = SlightlyLow(3, tsh, 10)
    private val fivePercentLow = SlightlyLow(4, tsh, 5)

    @Test
    fun id() {
        tenPercentLow.id shouldBe 3
    }

    @Test
    fun sameAs() {
        tenPercentLow should beSameAs(tenPercentLow)
        tenPercentLow should beSameAs(SlightlyLow(100, tenPercentLow.attribute, tenPercentLow.allowablePercentageBelowLowRangeCutoff))
        tenPercentLow should beSameAs(SlightlyLow(null, tenPercentLow.attribute, tenPercentLow.allowablePercentageBelowLowRangeCutoff))

        tenPercentLow shouldNot beSameAs(SlightlyHigh(null, tenPercentLow.attribute, tenPercentLow.allowablePercentageBelowLowRangeCutoff))
        tenPercentLow shouldNot beSameAs(SlightlyLow(null, tenPercentLow.attribute, 11))
        tenPercentLow shouldNot beSameAs(SlightlyLow(tenPercentLow.id, tenPercentLow.attribute, 11))
        tenPercentLow shouldNot beSameAs(SlightlyLow(null, glucose, tenPercentLow.allowablePercentageBelowLowRangeCutoff))
        tenPercentLow shouldNot beSameAs(SlightlyLow(tenPercentLow.id, glucose, tenPercentLow.allowablePercentageBelowLowRangeCutoff))
    }
    
    @Test
    fun allowedCutoffs() {
        checkExceptionThrownForCutoff(200)
        checkExceptionThrownForCutoff(110)
        checkExceptionThrownForCutoff(101)
        checkExceptionThrownForCutoff(100)
        checkExceptionThrownForCutoff(0)
        checkExceptionThrownForCutoff(-1)
        checkExceptionThrownForCutoff(-10)
    }

    private fun checkExceptionThrownForCutoff(cutoff: Int) {
        val exception = shouldThrow<IllegalArgumentException> {
            SlightlyLow(45, tsh, cutoff)
        }
        exception.message should startWith("Cutoff should be an integer in the range [1, 99]")
    }

    @Test
    fun attributeNotInCase() {
        tenPercentLow.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun valueHasNoReferenceRange() {
        tenPercentLow.holds(tshValueHasNoRangeCase()) shouldBe false
    }

    @Test
    fun rangeHasNoLowerBound() {
        val builder1 = RDRCaseBuilder()
        val range = ReferenceRange(null, "5.0")
        builder1.addResult(tsh, defaultDate,TestResult("0.4", range, "mmol/L"))
        val case1 = builder1.build("Case1")
        tenPercentLow.holds(case1) shouldBe false
    }

    @Test
    fun valueNormal() {
        tenPercentLow.holds(singleEpisodeCaseWithTSHNormal()) shouldBe false
        tenPercentLow.holds(twoEpisodeCaseWithBothTSHValuesNormal()) shouldBe false
        tenPercentLow.holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
    }

    @Test
    fun valueHigh() {
        tenPercentLow.holds(highTSHCase()) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        tenPercentLow.holds(tshValueNonNumericCase()) shouldBe false
        tenPercentLow.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
        tenPercentLow.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun cutoffsForTenPercentLow() {
        tenPercentLow.holds(createCase(1.00001, 1.0)) shouldBe false
        tenPercentLow.holds(createCase(1.00000, 1.0)) shouldBe false
        tenPercentLow.holds(createCase(0.9999, 1.0)) shouldBe true
        tenPercentLow.holds(createCase(0.95, 1.0)) shouldBe true
        tenPercentLow.holds(createCase(0.90001, 1.0)) shouldBe true
        tenPercentLow.holds(createCase(0.90000, 1.0)) shouldBe true
        tenPercentLow.holds(createCase(0.89999, 1.0)) shouldBe true
        tenPercentLow.holds(createCase(0.899, 1.0)) shouldBe false
        tenPercentLow.holds(createCase(0.8, 1.0)) shouldBe false
        tenPercentLow.holds(createCase(0.0, 1.0)) shouldBe false
    }

    @Test
    fun cutoffsForFivePercentLow() {
        fivePercentLow.holds(createCase(1.00001, 1.0)) shouldBe false
        fivePercentLow.holds(createCase(1.00000, 1.0)) shouldBe false
        fivePercentLow.holds(createCase(0.9999, 1.0)) shouldBe true
        fivePercentLow.holds(createCase(0.95001, 1.0)) shouldBe true
        fivePercentLow.holds(createCase(0.95000, 1.0)) shouldBe true
        fivePercentLow.holds(createCase(0.94999, 1.0)) shouldBe true // Close enough
        fivePercentLow.holds(createCase(0.949, 1.0)) shouldBe false
        fivePercentLow.holds(createCase(0.94, 1.0)) shouldBe false
    }

    private fun createCase(tshValue: Double, lowerBound: Double): RDRCase {
        val builder1 = RDRCaseBuilder()
        val referenceRange = ReferenceRange("$lowerBound", "10.0")
        builder1.addResult(tsh, defaultDate , TestResult("$tshValue", referenceRange, "pmol/L"))
        return builder1.build("Case")
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(tenPercentLow) shouldBe tenPercentLow
    }

    @Test
    fun asText() {
        tenPercentLow.asText() shouldBe "TSH is at most 10% low"
    }
}