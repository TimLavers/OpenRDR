package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedHighRangeCondition
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import kotlin.test.Test

class ExtendedHighRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        with(ExtendedHighRangeSuggestion(Current)) {
            invoke(tsh, null) shouldBe null
            invoke(tsh, tr("whatever")) shouldBe null
            invoke(tsh, tr("1.9")) shouldBe null
            invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldBe null
            invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldBe null
            invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldBe null
            invoke(tsh, tr("1.9", rr("4.0", null))) shouldBe null
            invoke(tsh, tr("10.9", rr("4.0", "10.0"), "mg/L")) shouldNotBe null
            invoke(tsh, tr("na", rr("4.0", "5.9"), "mg/L")) shouldBe null
        }

        with(ExtendedHighRangeSuggestion(All).invoke(tsh, tr("5.8", rr("1.5", "5.0")))!!) {
            initialSuggestion() shouldBe slightlyHigh(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedHighRangeCondition(tsh, All)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedHighRangeSuggestion(All).createEditableCondition(tsh) shouldBe EditableExtendedHighRangeCondition(tsh, All)
    }
}