package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class NonEditableSuggestedConditionTest: ConditionTestBase() {
    private val normal = EpisodicCondition(tsh, Normal, Current)
    private val nonEditableSuggestedCondition = NonEditableSuggestedCondition(normal)

    @Test
    fun serializationTest() {
        serializeDeserialize(nonEditableSuggestedCondition) shouldBe nonEditableSuggestedCondition
    }

    @Test
    fun isEditableTest() {
        nonEditableSuggestedCondition.isEditable() shouldBe false
    }

    @Test
    fun shouldBeUsedAtMostOncePerRuleTes() {
        nonEditableSuggestedCondition.shouldBeUsedAtMostOncePerRule() shouldBe true
    }

    @Test
    fun editableConditionTest() {
        nonEditableSuggestedCondition.editableCondition() shouldBe null
    }
}