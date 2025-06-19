package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class UndoRuleDescriptionTest {
    @Test
    fun serialisationTest() {
        val urd = UndoRuleDescription("An excellent rule", true)
        val deserialised = serializeDeserialize(urd)
        urd shouldBe deserialised
    }
}