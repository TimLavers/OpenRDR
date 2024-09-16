package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.HighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedHighRangeConditionTest: ConditionTestBase() {
    private val ercHighCurrent = EditableExtendedHighRangeCondition(tsh, Current)
    private val ercHighNone = EditableExtendedHighRangeCondition(tsh, No)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercHighCurrent) shouldBe ercHighCurrent
    }

    @Test
    fun fixedTextPart1() {
        ercHighCurrent.fixedTextPart1() shouldBe "${tsh.name} is high by at most "
    }

    @Test
    fun condition() {
        ercHighCurrent.condition("12") shouldBe EpisodicCondition(null, tsh, HighByAtMostSomePercentage(12), Current)
        ercHighNone.condition("12") shouldBe EpisodicCondition(null, tsh, HighByAtMostSomePercentage(12), No)
    }

    @Test
    fun equalsTest() {
        ercHighCurrent.equals(null) shouldBe false
        ercHighCurrent.equals(tsh) shouldBe false
        ercHighCurrent.equals(EditableExtendedHighRangeCondition(glucose, Current)) shouldBe false
        ercHighCurrent.equals(EditableExtendedHighRangeCondition(tsh, Current)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercHighCurrent.hashCode() shouldBe EditableExtendedHighRangeCondition(tsh, Current).hashCode()
    }

    @Test
    fun prerequisitePredicate() {
        ercHighCurrent.prerequisitePredicate() shouldBe High
        ercHighNone.prerequisitePredicate() shouldBe High
    }
}