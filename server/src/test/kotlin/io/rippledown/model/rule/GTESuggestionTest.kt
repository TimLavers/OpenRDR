package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.edit.EditableGTECondition
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.tr
import kotlin.test.Test

class GTESuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        GreaterThanOrEqualsSuggestion.invoke(tsh, null) shouldBe null
        GreaterThanOrEqualsSuggestion.invoke(tsh, tr("whatever")) shouldBe null
        with(GreaterThanOrEqualsSuggestion.invoke(tsh, tr("1.0"))!!) {
            initialSuggestion() shouldBe greaterThanOrEqualTo(null, tsh, 1.0)
            isEditable() shouldBe true
            editableCondition()shouldBe EditableGTECondition(tsh, EditableValue("1.0", Type.Real))
        }
    }
}