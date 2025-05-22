package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class TrueTest: ConditionTestBase() {

    @Test
    fun attributeNamesTest() {
        True.attributeNames() shouldBe emptySet()
    }

    @Test
    fun serializationTest() {
        serializeDeserialize(True) shouldBe True
    }

    @Test
    fun holdsForCaseTest() {
        True.holds(clinicalNotesCase("stuff")) shouldBe true
    }

    @Test
    fun idTest() {
        True.id shouldBe null
    }

    @Test
    fun asTextTest() {
        True.asText() shouldBe "TRUE"
    }

    @Test
    fun alignAttributesTest() {
        val newTSH = tsh.copy(name = "Renamed")
        val attributeMap = mapOf(tsh.id to newTSH, tsh.id + 1200 to tsh, clinicalNotes.id to clinicalNotes)
        val aligned = True.alignAttributes{id -> attributeMap[id]!! }
        aligned shouldBe True
    }

    @Test
    fun sameAsTest() {
        True.sameAs(True) shouldBe true
        True.sameAs(isNumeric(tsh)) shouldBe false
    }
}