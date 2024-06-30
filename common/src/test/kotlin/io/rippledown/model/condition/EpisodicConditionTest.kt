package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.TestResult
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.defaultDate
import kotlin.test.Test

class EpisodicConditionTest: ConditionTestBase() {

    private val tshLow = EpisodicCondition(123, tsh, Low, Current)

    @Test
    fun secondaryConstructor() {
        val nullId = EpisodicCondition(tsh, Low, Current)
        nullId.id shouldBe null
        nullId.attribute shouldBe tsh
        nullId.predicate shouldBe Low
        nullId.signature shouldBe Current
    }

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
        builder1.addResult(tsh, defaultDate , TestResult("0.067", tshRange, "pmol/L"))
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
    fun allLow() {
        val allTshLow = EpisodicCondition(null, tsh, Low, All)
        allTshLow.holds(threeEpisodeCaseWithEachTshLow()) shouldBe true
        allTshLow.holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
        allTshLow.asText() shouldBe "all ${tsh.name} are low"
    }

    @Test
    fun allContain() {
        val allContain = EpisodicCondition(null, clinicalNotes, Contains("cat"), All)
        allContain.holds(multiEpisodeClinicalNotesCase("dog", "cat", "horse")) shouldBe false
        allContain.holds(multiEpisodeClinicalNotesCase("scatter", "cat", "cathartic")) shouldBe true
    }

    @Test
    fun serialization() {
        serializeDeserialize(tshLow) shouldBe tshLow

        // One without an id.
        val idLess = EpisodicCondition(null, tsh, Low, Current)
        serializeDeserialize(idLess) shouldBe idLess
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
        // Identical.
        tshLow.sameAs(tshLow) shouldBe true

        // Same but for id.
        tshLow.sameAs(EpisodicCondition(null, tsh, Low, Current)) shouldBe true
        tshLow.sameAs(EpisodicCondition(88, tsh, Low, Current)) shouldBe true
        EpisodicCondition(88, tsh, Low, Current).sameAs(EpisodicCondition(88, tsh, Low, Current)) shouldBe true
        EpisodicCondition(123, tsh, Low, Current).sameAs(EpisodicCondition(88, tsh, Low, Current)) shouldBe true

        // Attribute different.
        tshLow.sameAs(EpisodicCondition(null, clinicalNotes, Low, Current)) shouldBe false

        // Predicate different.
        tshLow.sameAs(EpisodicCondition(null, tsh, Normal, Current)) shouldBe false

        // Chain different.
        tshLow.sameAs(EpisodicCondition(null, tsh, Low, All)) shouldBe false
    }
}