package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.containsText
import io.rippledown.model.condition.edit.EditableContainsCondition
import io.rippledown.model.condition.tr
import kotlin.test.Test

class ContainsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        ContainsSuggestion.invoke(notes, null) shouldBe null
        val whatever = "Whatever, times ten."
        with(ContainsSuggestion.invoke(notes, tr(whatever))!!) {
            isEditable() shouldBe true
            editableCondition() shouldBe EditableContainsCondition(notes, whatever)
            initialSuggestion() shouldBe containsText(null, notes, whatever)
        }
    }
}