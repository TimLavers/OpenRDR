package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.allDoNotContainText
import io.rippledown.model.condition.edit.EditableDoesNotContainCondition
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.tr
import kotlin.test.Test

class DoesNotContainSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        val whatever = "Whatever, times ten."
        DoesNotContainSuggestion(Current).invoke(notes, null) shouldBe null
        DoesNotContainSuggestion(Current).invoke(notes, tr(whatever)) shouldNotBe null
        with(DoesNotContainSuggestion(All).invoke(notes, tr(whatever))!!) {
            isEditable() shouldBe true
            editableCondition() shouldBe EditableDoesNotContainCondition(notes, All)
            initialSuggestion() shouldBe allDoNotContainText(null, notes, "")
        }
    }
}