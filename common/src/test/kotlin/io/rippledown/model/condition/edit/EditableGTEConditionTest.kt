package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableGTEConditionTest: ConditionTestBase() {

    @Test
    fun serializationTest() {
        val editableGTECondition = EditableGTECondition(tsh, EditableValue("0.67", Type.Real))
        serializeDeserialize(editableGTECondition) shouldBe editableGTECondition
    }
}