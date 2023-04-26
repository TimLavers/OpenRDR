package io.rippledown.model.condition

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import kotlin.test.Test

internal class IsHighTest: ConditionTestBase() {

    private val condition = IsHigh(1000, tsh)

    @Test
    fun id() {
        condition.id shouldBe 1000
    }

    @Test
    fun sameAs() {
        condition should beSameAs(condition)
        condition should beSameAs(IsHigh(100, condition.attribute))
        condition should beSameAs(IsHigh(null, condition.attribute))

        condition shouldNot beSameAs(IsLow(null, condition.attribute))
        condition shouldNot beSameAs(IsHigh(null, glucose))
        condition shouldNot beSameAs(IsHigh(condition.id, glucose))
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(condition) as IsHigh
        conditionCopy.attribute shouldNotBeSameInstanceAs condition.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs condition.attribute
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
        condition.holds(case) shouldBe false
    }

    @Test
    fun valueHigh() {
        condition.holds(highTSHCase()) shouldBe true
    }

    @Test
    fun valueHighAgainstOneSidedRange() {
        condition.holds(highTSHWithOneSidedRangeCase()) shouldBe true
    }

    @Test
    fun currentValueNormal() {
        condition.holds(twoEpisodeCaseWithFirstTSHHighSecondNormal()) shouldBe false
        condition.holds(twoEpisodeCaseWithFirstTSHNormalSecondHigh()) shouldBe true
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
        condition.asText() shouldBe "TSH is high"
    }
}