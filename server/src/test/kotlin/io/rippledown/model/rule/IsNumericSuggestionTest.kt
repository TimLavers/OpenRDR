package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.isNumeric
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

class IsNumericSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        IsNumericSuggestion().invoke(tsh, null) shouldBe null
        IsNumericSuggestion().invoke(tsh, tr("whatever")) shouldBe null

        with(IsNumericSuggestion().invoke(tsh, tr("1.2", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe isNumeric(tsh)
            isEditable() shouldBe false
            editableCondition() shouldBe null
        }
    }
}