package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.EditableGreaterThanEqualsCondition
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import io.rippledown.model.condition.episodic.signature.AtMost
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.tr
import kotlin.test.Test

class GreaterThanOrEqualsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        GreaterThanOrEqualsSuggestion(Current).invoke(tsh, null) shouldBe null
        GreaterThanOrEqualsSuggestion(Current).invoke(tsh, tr("whatever")) shouldBe null
        with(GreaterThanOrEqualsSuggestion(Current).invoke(tsh, tr("1.0"))!!) {
            initialSuggestion() shouldBe greaterThanOrEqualTo(null, tsh, 1.0)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableGreaterThanEqualsCondition(tsh, EditableValue("1.0", Type.Real), Current)
        }
        with(GreaterThanOrEqualsSuggestion(AtMost(3)).invoke(tsh, tr("1.0"))!!) {
            editableCondition() shouldBe EditableGreaterThanEqualsCondition(tsh, EditableValue("1.0", Type.Real), AtMost(3))
        }
    }
}