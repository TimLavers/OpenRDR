package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class AndTest: ConditionTestBase() {
    private val tshLow = EpisodicCondition(123, tsh, Low, Current)
    private val bondi = EpisodicCondition( 124, clinicalNotes, Contains("Bondi"), All)
    private val bronte = EpisodicCondition( 125, clinicalNotes, Contains("Bronte"), All)
    private val bondiLow = And(tshLow, bondi)
    private val bondiBronte = And(bondi, bronte)

    @Test
    fun attributeNamesTest() {
        bondiLow.attributeNames() shouldBe setOf(tshLow.attribute.name, bondi.attribute.name)
    }

    @Test
    fun serializationTest() {
        serializeDeserialize(bondiLow) shouldBe bondiLow
    }

    @Test
    fun holdsForCaseTest() {
        bondiLow.holds(clinicalNotesCase("stuff")) shouldBe false
        bondiLow.holds(clinicalNotesCase("Bondi")) shouldBe false
        bondiBronte.holds(clinicalNotesCase("Bondi")) shouldBe false
        bondiBronte.holds(clinicalNotesCase("Bondi and Bronte")) shouldBe true
    }

    @Test
    fun idTest() {
        bondiLow.id shouldBe null
    }

    @Test
    fun asTextTest() {
        bondiLow.asText() shouldBe "${tshLow.asText()} and ${bondi.asText()}"
    }

    @Test
    fun alignAttributesTest() {
        val newTSH = tsh.copy(name = "TSH Renamed")
        val newNotes = clinicalNotes.copy(name = "Notes Renamed")
        val attributeMap = mapOf(tsh.id to newTSH, tsh.id + 1200 to tsh, clinicalNotes.id to newNotes)
        val aligned = bondiLow.alignAttributes{id -> attributeMap[id]!! }
        (aligned.left as EpisodicCondition).attribute shouldBe newTSH
        (aligned.right as EpisodicCondition).attribute shouldBe newNotes
    }

    @Test
    fun sameAsTest() {
        bondiLow.sameAs(True) shouldBe false
        bondiLow.sameAs(tshLow) shouldBe false
        bondiLow.sameAs(bondiLow) shouldBe true
        bondiLow.sameAs(And(bondiLow.right, bondiLow.left)) shouldBe false
        bondiLow.sameAs(And(bondiLow.right, bondiLow.right)) shouldBe false
        bondiLow.sameAs(And(bondiLow.left, bondiLow.left)) shouldBe false
        bondiLow.sameAs(And(bondiLow.left, bondiLow.right)) shouldBe true
    }
}