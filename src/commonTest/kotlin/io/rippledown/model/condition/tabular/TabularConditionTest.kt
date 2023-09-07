package io.rippledown.model.condition.tabular

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.tabular.chain.Current
import io.rippledown.model.condition.tabular.predicate.Low
import io.rippledown.model.defaultDate
import kotlin.test.Test

class TabularConditionTest: ConditionTestBase() {

    private val tshLow = TabularCondition(123, tsh, Low, Current)

    @Test
    fun id() {
        tshLow.id shouldBe 123
    }

    @Test
    fun attributeNotInCase() {
        tshLow.holds(glucoseOnlyCase()) shouldBe false
    }

    @Test
    fun currentLow() {
        // No reference range.
        tshLow.holds(tshValueHasNoRangeCase()) shouldBe false
        // Value non-numeric.
        tshLow.holds(tshValueNonNumericCase()) shouldBe false
        // Value is normal.
        tshLow.holds(singleEpisodeCaseWithTSHNormal()) shouldBe false

        // Value is low.
        val builder1 = RDRCaseBuilder()
        builder1.addResult(tsh, defaultDate , TestResult("0.067", range, "pmol/L"))
        val case = builder1.build("Case")
        tshLow.holds(case) shouldBe true

        // Value is high.
        tshLow.holds(highTSHCase()) shouldBe false

        // Multiple episodes.
        tshLow.holds(twoEpisodeCaseWithFirstTSHHighSecondNormal()) shouldBe false
        tshLow.holds(twoEpisodeCaseWithFirstTSHNormalSecondHigh()) shouldBe false
        tshLow.holds(twoEpisodeCaseWithFirstTSHNormalSecondLow()) shouldBe true
        tshLow.holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false

        // Current value is blank.
        tshLow.holds(twoEpisodeCaseWithCurrentTSHValueBlank()) shouldBe false
    }

    @Test
    fun serialization() {
        serializeDeserialize(tshLow) shouldBe tshLow
    }

    @Test
    fun asText() {
        tshLow.asText() shouldBe "${tsh.name} is low"
    }

    @Test
    fun alignAttributes() {
        val conditionCopy = serializeDeserialize(tshLow)
        conditionCopy.attribute shouldNotBeSameInstanceAs tshLow.attribute
        val alignedCopy = conditionCopy.alignAttributes(::attributeForId)
        alignedCopy.attribute shouldBeSameInstanceAs tshLow.attribute
    }

    @Test
    fun sameAs() {
        tshLow.sameAs(tshLow) shouldBe true
        tshLow.sameAs(TabularCondition(null, tsh, Low, Current)) shouldBe true
    }
}