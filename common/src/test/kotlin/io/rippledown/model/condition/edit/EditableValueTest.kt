package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.Type.Text
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class EditableValueTest {
    @Test
    fun serializationTest() {
        val editableValue = EditableValue("Whatever", Text)
        serializeDeserialize(editableValue) shouldBe editableValue
    }
}