package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableLessThanEqualsConditionTest: ConditionTestBase() {
    private val lteCurrent = EditableLessThanEqualsCondition(tsh, EditableValue("0.67", Type.Real), Current)
    private val lteAtLeast2 = EditableLessThanEqualsCondition(tsh, EditableValue("0.67", Type.Real), AtLeast(2))

    @Test
    fun serializationTest() {
        serializeDeserialize(lteCurrent) shouldBe lteCurrent
    }

    @Test
    fun fixedTextPart1() {
        lteCurrent.fixedTextPart1() shouldBe "${tsh.name} ≤ "
    }

    @Test
    fun editableValue() {
        lteCurrent.editableValue() shouldBe EditableValue("0.67", Type.Real)
    }

    @Test
    fun condition() {
        lteCurrent.condition("123") shouldBe EpisodicCondition(null, tsh, LessThanOrEquals(123.0), Current)
        lteAtLeast2.condition("123") shouldBe EpisodicCondition(null, tsh, LessThanOrEquals(123.0), AtLeast(2))
    }
}