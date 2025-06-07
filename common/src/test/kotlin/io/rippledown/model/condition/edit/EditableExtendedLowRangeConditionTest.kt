package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.LowByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class EditableExtendedLowRangeConditionTest: ConditionTestBase() {
    private val ercLowCurrent = EditableExtendedLowRangeCondition(tsh, Current)
    private val ercLowAtLeast2 = EditableExtendedLowRangeCondition(tsh, AtLeast(2))

    @Test
    fun serializationTest() {
        serializeDeserialize(ercLowCurrent) shouldBe ercLowCurrent
        serializeDeserialize(ercLowAtLeast2) shouldBe ercLowAtLeast2
    }

    @Test
    fun fixedTextPart1() {
        ercLowCurrent.fixedTextPart1() shouldBe "${tsh.name} is low by at most "
    }

    @Test
    fun fixedTextPart2() {
        ercLowCurrent.fixedTextPart2() shouldBe "%"
    }

    @Test
    fun editableValue() {
        ercLowCurrent.editableValue() shouldBe EditableValue("10", Type.Integer)
    }

    @Test
    fun condition() {
        ercLowCurrent.condition("12") shouldBe EpisodicCondition(null, tsh, LowByAtMostSomePercentage(12), Current)
        ercLowAtLeast2.condition("12") shouldBe EpisodicCondition(null, tsh, LowByAtMostSomePercentage(12), AtLeast(2))
    }

    @Test
    fun equalsTest() {
        ercLowCurrent.equals(null) shouldBe false
        ercLowCurrent.equals(tsh) shouldBe false
        ercLowCurrent.equals(EditableExtendedLowRangeCondition(glucose, Current)) shouldBe false
        ercLowCurrent.equals(EditableExtendedLowRangeCondition(tsh, All)) shouldBe false
        ercLowCurrent.equals(EditableExtendedLowRangeCondition(tsh, Current)) shouldBe true

        ercLowAtLeast2.equals(EditableExtendedLowRangeCondition(tsh, Current)) shouldBe false
        ercLowAtLeast2.equals(EditableExtendedLowRangeCondition(tsh, AtLeast(1))) shouldBe false
        ercLowAtLeast2.equals(EditableExtendedLowRangeCondition(tsh, AtLeast(2))) shouldBe true
    }

    @Test
    fun prerequisite() {
        ercLowCurrent.prerequisite().holds(lowTSHCase()) shouldBe true
        ercLowCurrent.prerequisite().holds(normalTSHCase()) shouldBe false
        ercLowCurrent.prerequisite().holds(highTSHCase()) shouldBe false
        ercLowCurrent.prerequisite().holds(glucoseOnlyCase()) shouldBe false

        ercLowAtLeast2.prerequisite().holds(twoEpisodeCaseWithFirstTSHLowSecondNormal()) shouldBe false
        ercLowAtLeast2.prerequisite().holds(twoEpisodeCaseWithFirstTSHNormalSecondLow()) shouldBe false
        ercLowAtLeast2.prerequisite().holds(threeEpisodeCaseWithEachTshLow()) shouldBe true
    }

    @Test
    fun prerequisitePredicate() {
        ercLowCurrent.prerequisitePredicate() shouldBe Low
        ercLowAtLeast2.prerequisitePredicate() shouldBe Low
    }

    @Test
    fun hashCodeTest() {
        ercLowCurrent.hashCode() shouldBe EditableExtendedLowRangeCondition(tsh, Current).hashCode()
    }
}