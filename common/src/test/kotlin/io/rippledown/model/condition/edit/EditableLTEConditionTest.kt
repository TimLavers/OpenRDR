package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableLTEConditionTest: ConditionTestBase() {
    private val lte = EditableLTECondition(tsh, EditableValue("0.67", Type.Real))

    @Test
    fun serializationTest() {
        serializeDeserialize(lte) shouldBe lte
    }

    @Test
    fun fixedTextPart1() {
        lte.fixedTextPart1() shouldBe "${tsh.name} ≤ "
    }

    @Test
    fun editableValue() {
        lte.editableValue() shouldBe EditableValue("0.67", Type.Real)
    }

    @Test
    fun condition() {
        lte.condition("123") shouldBe EpisodicCondition(null, tsh, LessThanOrEquals(123.0), Current)
    }
}