package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class ConditionListTest: ConditionTestBase() {

    @Test
    fun serialization() {
        val gte = EditableGreaterThanEqualsCondition(tsh, EditableValue("0.67", Type.Real), Current)
        val esc0 = EditableSuggestedCondition(gte)
        val normal = EpisodicCondition(tsh, Normal, Current)
        val esc1 = NonEditableSuggestedCondition(normal)
        val ehn = EditableExtendedHighNormalRangeCondition(tsh)
        val esc2 = EditableSuggestedCondition(ehn)

        val conditionList = ConditionList(listOf(esc0, esc1, esc2))
        serializeDeserialize(conditionList) shouldBe conditionList
    }
}