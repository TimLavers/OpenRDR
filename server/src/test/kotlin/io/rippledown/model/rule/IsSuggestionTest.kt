package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.isCondition
import io.rippledown.model.condition.tr
import kotlin.test.Test

class IsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        IsSuggestion.invoke(tsh, null) shouldBe null
        with(IsSuggestion.invoke(tsh, tr("whatever"))!!) {
            isEditable() shouldBe false
            editableCondition() shouldBe null
            initialSuggestion() shouldBe isCondition(null, tsh, "whatever")
        }
    }
}