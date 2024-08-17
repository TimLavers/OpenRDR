package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableContainsConditionTest: ConditionTestBase() {
    private val condition = EditableContainsCondition(clinicalNotes, "stuff")

    @Test
    fun serializationTest() {
        serializeDeserialize(condition) shouldBe condition
    }

    @Test
    fun fixedTextPart1() {
        condition.fixedTextPart1() shouldBe "${clinicalNotes.name} contains "
    }

    @Test
    fun fixedTextPart2() {
        condition.fixedTextPart2() shouldBe ""
    }

    @Test
    fun editableValue() {
        condition.editableValue() shouldBe EditableValue("stuff", Type.Text)
    }

    @Test
    fun condition() {
        condition.condition("things") shouldBe EpisodicCondition(null, clinicalNotes, Contains("things"), Current)
    }
}