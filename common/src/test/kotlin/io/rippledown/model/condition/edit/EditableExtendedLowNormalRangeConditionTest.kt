package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LowOrNormal
import io.rippledown.model.condition.episodic.predicate.NormalOrLowByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedLowNormalRangeConditionTest: ConditionTestBase() {
    private val ercLowNormalCurrent = EditableExtendedLowNormalRangeCondition(tsh, Current)
    private val ercLowNormalAtMost1 = EditableExtendedLowNormalRangeCondition(tsh, AtMost(1))

    @Test
    fun serializationTest() {
        serializeDeserialize(ercLowNormalCurrent) shouldBe ercLowNormalCurrent
        serializeDeserialize(ercLowNormalAtMost1) shouldBe ercLowNormalAtMost1
    }

    @Test
    fun fixedTextPart1() {
        ercLowNormalCurrent.fixedTextPart1() shouldBe "${tsh.name} is normal or low by at most "
    }

    @Test
    fun fixedTextPart2() {
        ercLowNormalCurrent.fixedTextPart2() shouldBe "%"
    }

    @Test
    fun editableValue() {
        ercLowNormalCurrent.editableValue() shouldBe EditableValue("10", Type.Integer)
    }

    @Test
    fun condition() {
        ercLowNormalCurrent.condition("12") shouldBe EpisodicCondition(
            null,
            tsh,
            NormalOrLowByAtMostSomePercentage(12),
            Current
        )
    }

    @Test
    fun equalsTest() {
        ercLowNormalCurrent.equals(null) shouldBe false
        ercLowNormalCurrent.equals(tsh) shouldBe false
        ercLowNormalCurrent.equals(EditableExtendedLowNormalRangeCondition(glucose, Current)) shouldBe false
        ercLowNormalCurrent.equals(EditableExtendedLowNormalRangeCondition(tsh, All)) shouldBe false
        ercLowNormalCurrent.equals(EditableExtendedLowNormalRangeCondition(tsh, Current)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercLowNormalCurrent.hashCode() shouldBe EditableExtendedLowNormalRangeCondition(tsh, Current).hashCode()
    }

    @Test
    fun prerequisitePredicate() {
        ercLowNormalCurrent.prerequisitePredicate() shouldBe LowOrNormal
        ercLowNormalAtMost1.prerequisitePredicate() shouldBe LowOrNormal
    }
}