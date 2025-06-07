package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.Type.*
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class TypeTest {
    @Test
    fun serializationTest() {
        serializeDeserialize(Real) shouldBe Real
        serializeDeserialize(Text) shouldBe Text
        serializeDeserialize(Integer) shouldBe Integer
    }

    @Test
    fun validTest() {
        Real.valid("pi") shouldBe false
        Real.valid("") shouldBe false
        Real.valid("two") shouldBe false
        Real.valid("3.145") shouldBe true
        Real.valid("300") shouldBe true

        Text.valid("") shouldBe true
        Text.valid("whatever") shouldBe true
        Text.valid("10") shouldBe true
        Text.valid("10.00") shouldBe true

        Integer.valid("") shouldBe false
        Integer.valid("whatever") shouldBe false
        Integer.valid("ten") shouldBe false
        Integer.valid("10.00") shouldBe false
        Integer.valid("10") shouldBe true
    }

    @Test
    fun convertTest() {
        Real.convert("blah") shouldBe null
        (Real.convert("12.34") as Double).compareTo(12.34) shouldBe 0

        Text.convert("whatever") shouldBe "whatever"

        Integer.convert("blah") shouldBe null
        (Integer.convert("10") as Int) shouldBe  10
    }
}