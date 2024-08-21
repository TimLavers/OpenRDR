package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableGTEConditionTest: ConditionTestBase() {
    private val gte = EditableGTECondition(tsh, EditableValue("0.67", Type.Real))

    @Test
    fun serializationTest() {
        serializeDeserialize(gte) shouldBe gte
    }

    @Test
    fun fixedTextPart1() {
        gte.fixedTextPart1() shouldBe "${tsh.name} ≥ "
    }

    @Test
    fun fixedTextPart2() {
        gte.fixedTextPart2() shouldBe ""
    }

    @Test
    fun editableValue() {
        gte.editableValue() shouldBe EditableValue("0.67", Type.Real)
    }

    @Test
    fun condition() {
        gte.condition("123") shouldBe EpisodicCondition(null, tsh, GreaterThanOrEquals(123.0), Current)
    }
}