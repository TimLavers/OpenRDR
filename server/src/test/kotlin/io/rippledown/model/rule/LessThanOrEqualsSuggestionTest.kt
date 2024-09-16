package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.EditableLessThanEqualsCondition
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.condition.tr
import kotlin.test.Test

class LessThanOrEqualsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        LessThanOrEqualsSuggestion(Current).invoke(tsh, null) shouldBe null
        LessThanOrEqualsSuggestion(Current).invoke(tsh, tr("whatever")) shouldBe null
        with(LessThanOrEqualsSuggestion(Current).invoke(tsh, tr("1.0"))!!) {
            initialSuggestion() shouldBe lessThanOrEqualTo(null, tsh, 1.0)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableLessThanEqualsCondition(tsh, EditableValue("1.0", Type.Real))
        }
    }
}