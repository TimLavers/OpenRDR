package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableSuggestedConditionTest: ConditionTestBase() {
    private val gte = EditableGTECondition(tsh, EditableValue("0.67", Type.Real))
    private val esc = EditableSuggestedCondition(gte)

    @Test
    fun serializationTest() {
        serializeDeserialize(esc) shouldBe esc
        val ehn = EditableExtendedHighNormalRangeCondition(tsh)
        val esc2 = EditableSuggestedCondition(ehn)
        serializeDeserialize(esc2) shouldBe esc2
    }

    @Test
    fun isEditableTest() {
        esc.isEditable() shouldBe true
    }

    @Test
    fun shouldBeUsedAtMostOncePerRuleTes() {
        esc.shouldBeUsedAtMostOncePerRule() shouldBe true
        val contains = EditableContainsCondition(tsh, "blah")
        EditableSuggestedCondition(contains).shouldBeUsedAtMostOncePerRule() shouldBe false
    }

    @Test
    fun editableConditionTest() {
        esc.editableCondition() shouldBe EditableGTECondition(tsh, EditableValue("0.67", Type.Real))
    }
}