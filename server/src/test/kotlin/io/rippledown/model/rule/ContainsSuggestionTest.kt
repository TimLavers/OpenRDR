package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.edit.EditableContainsCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.tr
import kotlin.test.Test

class ContainsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        ContainsSuggestion(Current).invoke(notes, null) shouldBe null
        val whatever = "Whatever, times ten."
        with(ContainsSuggestion(Current).invoke(notes, tr(whatever))!!) {
            isEditable() shouldBe true
            editableCondition() shouldBe EditableContainsCondition(notes, whatever)
            initialSuggestion() shouldBe containsText(null, notes, whatever)
        }
        with(ContainsSuggestion(AtMost(4)).invoke(notes, tr(whatever))!!) {
            isEditable() shouldBe true
            editableCondition() shouldBe EditableContainsCondition(notes, whatever, AtMost(4))
            initialSuggestion() shouldBe EpisodicCondition(notes, Contains(whatever), AtMost(4))
        }
    }
}