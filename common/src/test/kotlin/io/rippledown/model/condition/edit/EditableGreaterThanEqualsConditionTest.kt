package io.rippledown.model.condition.edit

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.ConditionTestBase
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.signature.AtLeast
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class EditableGreaterThanEqualsConditionTest: ConditionTestBase() {
    private val gteCurrent = EditableGreaterThanEqualsCondition(glucose, EditableValue("0.67", Type.Real), Current)
    private val gteAtMost3 = EditableGreaterThanEqualsCondition(glucose, EditableValue("100.0", Type.Real), AtMost(3))
    private val gteAtLeast3 = EditableGreaterThanEqualsCondition(glucose, EditableValue("100.0", Type.Real), AtLeast(3))

    @Test
    fun serializationTest() {
        serializeDeserialize(gteCurrent) shouldBe gteCurrent
        serializeDeserialize(gteAtMost3) shouldBe gteAtMost3
    }

    @Test
    fun prerequisite() {
        gteCurrent.prerequisite() shouldBe EpisodicCondition(glucose, IsNumeric, Current)
        gteCurrent.prerequisite().holds(clinicalNotesCase("")) shouldBe false
        gteCurrent.prerequisite().holds(glucoseOnlyCase("0.8")) shouldBe true
        gteCurrent.prerequisite().holds(glucoseOnlyCase("")) shouldBe false
        gteCurrent.prerequisite().holds(glucoseOnlyCase("not numeric")) shouldBe false
        gteCurrent.prerequisite().holds(multiEpisodeGlucoseCase("no", "1", "-1", "2")) shouldBe true
        gteCurrent.prerequisite().holds(multiEpisodeGlucoseCase("no", "1", "-1", "")) shouldBe false

        gteAtMost3.prerequisite() shouldBe EpisodicCondition(glucose, IsNumeric, AtMost(3))

        gteAtLeast3.prerequisite() shouldBe EpisodicCondition(glucose, IsNumeric, AtLeast(3))
        gteAtLeast3.prerequisite().holds(glucoseOnlyCase()) shouldBe false
        gteAtLeast3.prerequisite().holds(multiEpisodeGlucoseCase("no", "1", "-1", "")) shouldBe false
        gteAtLeast3.prerequisite().holds(multiEpisodeGlucoseCase("no", "1", "-1", "2")) shouldBe true
        gteAtLeast3.prerequisite().holds(multiEpisodeGlucoseCase("3", "1", "-1", "blah")) shouldBe true
    }

    @Test
    fun fixedTextPart1() {
        gteCurrent.fixedTextPart1() shouldBe "${tsh.name} ≥ "
        gteAtMost3.fixedTextPart1() shouldBe "${tsh.name} ≥ "
    }

    @Test
    fun fixedTextPart2() {
        gteCurrent.fixedTextPart2() shouldBe ""
    }

    @Test
    fun editableValue() {
        gteCurrent.editableValue() shouldBe EditableValue("0.67", Type.Real)
    }

    @Test
    fun condition() {
        gteCurrent.condition("123") shouldBe EpisodicCondition(null, tsh, GreaterThanOrEquals(123.0), Current)
    }
}