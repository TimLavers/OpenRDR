package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class IsLowTest: ConditionTestBase() {

    private val condition = IsLow(1000, tsh)

    @Test
    fun id() {
        condition.id shouldBe 1000
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as IsLow
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(IsLow(100, condition.attribute))
        condition should beSameAs(IsLow(null, condition.attribute))

        condition shouldNot beSameAs(IsHigh(null, condition.attribute))
        condition shouldNot beSameAs(IsLow(null, glucose))
        condition shouldNot beSameAs(IsLow(condition.id, glucose))
    }

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