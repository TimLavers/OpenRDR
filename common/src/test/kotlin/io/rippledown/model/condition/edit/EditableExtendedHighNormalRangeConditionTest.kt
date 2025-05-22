package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.HighOrNormal
import io.rippledown.model.condition.episodic.predicate.NormalOrHighByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class EditableExtendedHighNormalRangeConditionTest: ConditionTestBase() {
    private val ercHighNormalCurrent = EditableExtendedHighNormalRangeCondition(tsh, Current)
    private val ercHighNormalAll = EditableExtendedHighNormalRangeCondition(tsh, All)

    @Test
    fun serializationTest() {
        serializeDeserialize(ercHighNormalCurrent) shouldBe ercHighNormalCurrent
        serializeDeserialize(ercHighNormalAll) shouldBe ercHighNormalAll
    }

    @Test
    fun fixedTextPart1() {
        ercHighNormalCurrent.fixedTextPart1() shouldBe "${tsh.name} is normal or high by at most "
    }

    @Test
    fun condition() {
        ercHighNormalCurrent.condition("12") shouldBe EpisodicCondition(
            null,
            tsh,
            NormalOrHighByAtMostSomePercentage(12),
            Current
        )
        ercHighNormalAll.condition("12") shouldBe EpisodicCondition(
            null,
            tsh,
            NormalOrHighByAtMostSomePercentage(12),
            All
        )
    }

    @Test
    fun equalsTest() {
        ercHighNormalCurrent.equals(null) shouldBe false
        ercHighNormalCurrent.equals(tsh) shouldBe false
        ercHighNormalCurrent.equals(EditableExtendedHighNormalRangeCondition(glucose, Current)) shouldBe false
        ercHighNormalCurrent.equals(EditableExtendedHighNormalRangeCondition(tsh, No)) shouldBe false
        ercHighNormalCurrent.equals(EditableExtendedHighNormalRangeCondition(tsh, Current)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        ercHighNormalCurrent.hashCode() shouldBe EditableExtendedHighNormalRangeCondition(tsh, Current).hashCode()
    }

    @Test
    fun prerequisitePredicate() {
        ercHighNormalCurrent.prerequisitePredicate() shouldBe HighOrNormal
        ercHighNormalAll.prerequisitePredicate() shouldBe HighOrNormal
    }
}