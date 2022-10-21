package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

internal class IsLowTest: ConditionTestBase() {

    private val condition = IsLow(tsh)

    @Test
    fun attributeNotInCase() {
        condition.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun valueHasNoRange() {
        condition.holds(tshValueHasNoRangeCase()) shouldBe false
    }

    @Test
    fun valueNonNumeric() {
        condition.holds(tshValueNonNumericCase()) shouldBe false
    }

    @Test
    fun valueNormal() {
        condition.holds(singleEpisodeCaseWithTSHNormal()) shouldBe false
    }

    @Test
    fun valueLow() {
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.067", range, "pmol/L"))
        val case = builder1.build("Case")
        condition.holds(case) shouldBe true
    }

    @Test
    fun valueHigh() {
        condition.holds(highTSHCase()) shouldBe false
    }

    @Test
    fun currentValueNormal() {
        condition.holds(twoEpisodeCaseWithFirstTSHHighSecondNormal()) shouldBe false
        condition.holds(twoEpisodeCaseWithFirstTSHNormalSecondHigh()) shouldBe false
        condition.holds(twoEpisodeCaseWithFirstTSHNormalSecondLow()) shouldBe true
        condition.holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
    }

    @Test
    fun currentValueBlank() {
        condition.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun jsonSerialisation() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun asText() {
        condition.asText() shouldBe "TSH is low"
    }
}