package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableContainsConditionTest: ConditionTestBase() {
    private val currentValueCondition = EditableContainsCondition(clinicalNotes, "stuff")
    private val allValuesCondition = EditableContainsCondition(clinicalNotes, "stuff", All)
    private val noValuesCondition = EditableContainsCondition(clinicalNotes, "stuff", No)

    @Test
    fun serializationTest() {
        serializeDeserialize(currentValueCondition) shouldBe currentValueCondition
        serializeDeserialize(noValuesCondition) shouldBe noValuesCondition
        serializeDeserialize(allValuesCondition) shouldBe allValuesCondition
    }

    @Test
    fun fixedTextPart1() {
        currentValueCondition.fixedTextPart1() shouldBe "${clinicalNotes.name} contains "
        allValuesCondition.fixedTextPart1() shouldBe "all ${clinicalNotes.name} contain "
        noValuesCondition.fixedTextPart1() shouldBe "no ${clinicalNotes.name} contains "
    }

    @Test
    fun fixedTextPart2() {
        currentValueCondition.fixedTextPart2() shouldBe ""
    }

    @Test
    fun shouldBeUsedAtMostOncePerRule() {
        currentValueCondition.shouldBeUsedAtMostOncePerRule() shouldBe false
    }

    @Test
    fun editableValue() {
        currentValueCondition.editableValue() shouldBe EditableValue("stuff", Type.Text)
    }

    @Test
    fun condition() {
        currentValueCondition.condition("things") shouldBe EpisodicCondition(null, clinicalNotes, Contains("things"), Current)
        allValuesCondition.condition("things") shouldBe EpisodicCondition(null, clinicalNotes, Contains("things"), All)
    }

    @Test
    fun prerequisite() {
        with(currentValueCondition.prerequisite()) {
            attribute shouldBe clinicalNotes
            signature shouldBe Current
            predicate shouldBe IsNotBlank
            holds(clinicalNotesCase("present")) shouldBe true
            holds(clinicalNotesCase("")) shouldBe false
            holds(glucoseOnlyCase()) shouldBe false
        }

        with(allValuesCondition.prerequisite()) {
            attribute shouldBe clinicalNotes
            signature shouldBe All
            predicate shouldBe IsNotBlank
            holds(clinicalNotesCase("present")) shouldBe true
            holds(clinicalNotesCase("")) shouldBe false
            holds(multiEpisodeClinicalNotesCase("yes", "maybe", "no")) shouldBe true
            holds(multiEpisodeClinicalNotesCase("yes", "", "no")) shouldBe false
            holds(glucoseOnlyCase()) shouldBe false
        }

    }
}