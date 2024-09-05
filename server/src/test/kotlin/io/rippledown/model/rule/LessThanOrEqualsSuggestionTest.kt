package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.EditableLTECondition
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import io.rippledown.model.condition.lessThanOrEqualTo
import io.rippledown.model.condition.tr
import kotlin.test.Test

class LessThanOrEqualsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        LessThanOrEqualsSuggestion.invoke(tsh, null) shouldBe null
        LessThanOrEqualsSuggestion.invoke(tsh, tr("whatever")) shouldBe null
        with(LessThanOrEqualsSuggestion.invoke(tsh, tr("1.0"))!!) {
            initialSuggestion() shouldBe lessThanOrEqualTo(null, tsh, 1.0)
            isEditable() shouldBe true
            editableCondition()shouldBe EditableLTECondition(tsh, EditableValue("1.0", Type.Real))
        }
    }
}