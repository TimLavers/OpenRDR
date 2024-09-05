package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LowByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedLowRangeConditionTest: ConditionTestBase() {
    private val ercLow = EditableExtendedLowRangeCondition(tsh)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercLow) shouldBe ercLow
    }

    @Test
    fun fixedTextPart1() {
        ercLow.fixedTextPart1() shouldBe "${tsh.name} is low by at most "
    }

    @Test
    fun fixedTextPart2() {
        ercLow.fixedTextPart2() shouldBe "%"
    }

    @Test
    fun editableValue() {
        ercLow.editableValue() shouldBe EditableValue("10", Type.Integer)
    }

    @Test
    fun condition() {
        ercLow.condition("12") shouldBe EpisodicCondition(null, tsh, LowByAtMostSomePercentage(12), Current)
    }

    @Test
    fun equalsTest() {
        ercLow.equals(null) shouldBe false
        ercLow.equals(tsh) shouldBe false
        ercLow.equals(EditableExtendedLowRangeCondition(glucose)) shouldBe false
        ercLow.equals(EditableExtendedLowRangeCondition(tsh)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercLow.hashCode() shouldBe EditableExtendedLowRangeCondition(tsh).hashCode()
    }
}