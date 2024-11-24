package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.DoesNotContain
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableDoesNotContainConditionTest: ConditionTestBase() {
    private val currentValueCondition = EditableDoesNotContainCondition(clinicalNotes)
    private val allValuesCondition = EditableDoesNotContainCondition(clinicalNotes, All)
    private val noValuesCondition = EditableDoesNotContainCondition(clinicalNotes, No)

    @Test
    fun serializationTest() {
        serializeDeserialize(currentValueCondition) shouldBe currentValueCondition
        serializeDeserialize(noValuesCondition) shouldBe noValuesCondition
        serializeDeserialize(allValuesCondition) shouldBe allValuesCondition
    }

    @Test
    fun fixedTextPart1() {
        currentValueCondition.fixedTextPart1() shouldBe "${clinicalNotes.name} does not contain "
        allValuesCondition.fixedTextPart1() shouldBe "all ${clinicalNotes.name} do not contain "
        noValuesCondition.fixedTextPart1() shouldBe "no ${clinicalNotes.name} does not contain "
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
        currentValueCondition.editableValue() shouldBe EditableValue("", Type.Text)
    }

    @Test
    fun condition() {
        currentValueCondition.condition("things") shouldBe EpisodicCondition(
            null,
            clinicalNotes,
            DoesNotContain("things"),
            Current
        )
        allValuesCondition.condition("things") shouldBe EpisodicCondition(
            null,
            clinicalNotes,
            DoesNotContain("things"),
            All
        )
    }
}