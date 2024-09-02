package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.NormalOrLowByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedLowNormalRangeConditionTest: ConditionTestBase() {
    private val ercLowNormal = EditableExtendedLowNormalRangeCondition(tsh)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercLowNormal) shouldBe ercLowNormal
    }

    @Test
    fun fixedTextPart1() {
        ercLowNormal.fixedTextPart1() shouldBe "${tsh.name} is normal or low by at most "
    }

    @Test
    fun fixedTextPart2() {
        ercLowNormal.fixedTextPart2() shouldBe "%"
    }

    @Test
    fun editableValue() {
        ercLowNormal.editableValue() shouldBe EditableValue("10", Type.Integer)
    }

    @Test
    fun condition() {
        ercLowNormal.condition("12") shouldBe EpisodicCondition(null, tsh, NormalOrLowByAtMostSomePercentage(12), Current)
    }

    @Test
    fun equalsTest() {
        ercLowNormal.equals(null) shouldBe false
        ercLowNormal.equals(tsh) shouldBe false
        ercLowNormal.equals(EditableExtendedLowNormalRangeCondition(glucose)) shouldBe false
        ercLowNormal.equals(EditableExtendedLowNormalRangeCondition(tsh)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercLowNormal.hashCode() shouldBe EditableExtendedLowNormalRangeCondition(tsh).hashCode()
    }
}