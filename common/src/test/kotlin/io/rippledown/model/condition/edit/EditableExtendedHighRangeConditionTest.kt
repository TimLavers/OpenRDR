package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.HighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedHighRangeConditionTest: ConditionTestBase() {
    private val ercHigh = EditableExtendedHighRangeCondition(tsh)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercHigh) shouldBe ercHigh
    }

    @Test
    fun fixedTextPart1() {
        ercHigh.fixedTextPart1() shouldBe "${tsh.name} is high by at most "
    }

    @Test
    fun condition() {
        ercHigh.condition("12") shouldBe EpisodicCondition(null, tsh, HighByAtMostSomePercentage(12), Current)
    }

    @Test
    fun equalsTest() {
        ercHigh.equals(null) shouldBe false
        ercHigh.equals(tsh) shouldBe false
        ercHigh.equals(EditableExtendedHighRangeCondition(glucose)) shouldBe false
        ercHigh.equals(EditableExtendedHighRangeCondition(tsh)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercHigh.hashCode() shouldBe EditableExtendedHighRangeCondition(tsh).hashCode()
    }
}