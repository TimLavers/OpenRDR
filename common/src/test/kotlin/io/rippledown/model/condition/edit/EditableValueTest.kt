package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.Type.*
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableValueTest {
    @Test
    fun serializationTest() {
        val editableValue = EditableValue("Whatever", Text)
        serializeDeserialize(editableValue) shouldBe editableValue
    }
}