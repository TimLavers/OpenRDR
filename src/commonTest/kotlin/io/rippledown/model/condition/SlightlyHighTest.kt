package io.rippledown.model.condition

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.startWith
import io.rippledown.model.*
import kotlin.test.Test

internal class SlightlyHighTest: ConditionTestBase() {

    private val tenPercentHigh = SlightlyHigh(100, tsh, 10)
    private val fivePercentHigh = SlightlyHigh(500, tsh, 5)

    @Test
    fun id() {
        tenPercentHigh.id shouldBe 100
    }

    @Test
    fun sameAs() {
        tenPercentHigh should beSameAs(tenPercentHigh)
        tenPercentHigh should beSameAs(SlightlyHigh(100, tenPercentHigh.attribute, tenPercentHigh.allowablePercentageAboveHighRangeCutoff))
        tenPercentHigh should beSameAs(SlightlyHigh(null, tenPercentHigh.attribute, tenPercentHigh.allowablePercentageAboveHighRangeCutoff))

        tenPercentHigh shouldNot beSameAs(SlightlyLow(null, tenPercentHigh.attribute, tenPercentHigh.allowablePercentageAboveHighRangeCutoff))
        tenPercentHigh shouldNot beSameAs(SlightlyHigh(null, tenPercentHigh.attribute, 11))
        tenPercentHigh shouldNot beSameAs(SlightlyHigh(tenPercentHigh.id, tenPercentHigh.attribute, 11))
        tenPercentHigh shouldNot beSameAs(SlightlyHigh(null, glucose, tenPercentHigh.allowablePercentageAboveHighRangeCutoff))
        tenPercentHigh shouldNot beSameAs(SlightlyHigh(tenPercentHigh.id, glucose, tenPercentHigh.allowablePercentageAboveHighRangeCutoff))
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
            SlightlyLow(88, tsh, cutoff)
        }
        exception.message should startWith("Cutoff should be an integer in the range [1, 99]")
    }

    @Test
    fun attributeNotInCase() {
        tenPercentHigh.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun valueHasNoReferenceRange() {
        tenPercentHigh.holds(tshValueHasNoRangeCase()) shouldBe false
    }

    @Test
    fun rangeHasNoUpperBound() {
        val builder1 = RDRCaseBuilder()
        val range = ReferenceRange("5.0", null)
        builder1.addResult(tsh, defaultDate,TestResult("12.0", range, "mmol/L"))
        val case1 = builder1.build("Case1")
        tenPercentHigh.holds(case1) shouldBe false
    }

    @Test
    fun valueNormal() {
        tenPercentHigh.holds(singleEpisodeCaseWithTSHNormal()) shouldBe false
        tenPercentHigh.holds(twoEpisodeCaseWithBothTSHValuesNormal()) shouldBe false
        tenPercentHigh.holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
    }

    @Test
    fun valueHigh() {
        tenPercentHigh.holds(lowTSHCase()) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        tenPercentHigh.holds(tshValueNonNumericCase()) shouldBe false
        tenPercentHigh.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
        tenPercentHigh.holds(twoEpisodeCaseWithCurrentTSHValueNonNumeric()) shouldBe false
    }

    @Test
    fun cutoffsForTenPercentHigh() {
        tenPercentHigh.holds(createCase(1.90, 1.0)) shouldBe false
        tenPercentHigh.holds(createCase(1.11, 1.0)) shouldBe false
        tenPercentHigh.holds(createCase(1.101, 1.0)) shouldBe false
        tenPercentHigh.holds(createCase(1.10, 1.0)) shouldBe true
        tenPercentHigh.holds(createCase(1.05, 1.0)) shouldBe true
        tenPercentHigh.holds(createCase(1.00001, 1.0)) shouldBe true
        tenPercentHigh.holds(createCase(1.00000, 1.0)) shouldBe false
        tenPercentHigh.holds(createCase(0.9999, 1.0)) shouldBe false
        tenPercentHigh.holds(createCase(0.95, 1.0)) shouldBe false

        tenPercentHigh.holds(createCase(22.01, 20.0)) shouldBe false
        tenPercentHigh.holds(createCase(22.0, 20.0)) shouldBe true
        tenPercentHigh.holds(createCase(21.999, 20.0)) shouldBe true
    }

    @Test
    fun cutoffsForFivePercentHigh() {
        fivePercentHigh.holds(createCase(1.05, 1.0)) shouldBe true
        fivePercentHigh.holds(createCase(1.049, 1.0)) shouldBe true
        fivePercentHigh.holds(createCase(1.00001, 1.0)) shouldBe true
        fivePercentHigh.holds(createCase(1.00000, 1.0)) shouldBe false
        fivePercentHigh.holds(createCase(0.9999, 1.0)) shouldBe false
    }

    private fun createCase(tshValue: Double, upperBound: Double): RDRCase {
        val builder1 = RDRCaseBuilder()
        val referenceRange = ReferenceRange("0.1", "$upperBound")
        builder1.addResult(tsh, defaultDate , TestResult("$tshValue", referenceRange, "pmol/L"))
        return builder1.build("Case")
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(tenPercentHigh) shouldBe tenPercentHigh
    }

    @Test
    fun asText() {
        tenPercentHigh.asText() shouldBe "TSH is at most 10% high"
    }
}