package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.NormalOrHighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableExtendedHighNormalRangeConditionTest: ConditionTestBase() {
    private val ercHighNormal = EditableExtendedHighNormalRangeCondition(tsh)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercHighNormal) shouldBe ercHighNormal
    }

    @Test
    fun fixedTextPart1() {
        ercHighNormal.fixedTextPart1() shouldBe "${tsh.name} is normal or high by at most "
    }

    @Test
    fun condition() {
        ercHighNormal.condition("12") shouldBe EpisodicCondition(null, tsh, NormalOrHighByAtMostSomePercentage(12), Current)
    }

    @Test
    fun equalsTest() {
        ercHighNormal.equals(null) shouldBe false
        ercHighNormal.equals(tsh) shouldBe false
        ercHighNormal.equals(EditableExtendedHighNormalRangeCondition(glucose)) shouldBe false
        ercHighNormal.equals(EditableExtendedHighNormalRangeCondition(tsh)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercHighNormal.hashCode() shouldBe EditableExtendedHighNormalRangeCondition(tsh).hashCode()
    }
}