package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.Type.*
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class TypeTest {
    @Test
    fun serializationTest() {
        serializeDeserialize(Real) shouldBe Real
        serializeDeserialize(Text) shouldBe Text
        serializeDeserialize(Integer) shouldBe Integer
    }
}