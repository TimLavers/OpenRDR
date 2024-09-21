package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedHighNormalRangeCondition
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import kotlin.test.Test

class ExtendedHighNormalRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        with(ExtendedHighNormalRangeSuggestion(Current)) {
            invoke(tsh, null) shouldBe null
            invoke(tsh, tr("whatever")) shouldNotBe null
            invoke(tsh, tr("1.9")) shouldNotBe null
            invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldNotBe null
            invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldNotBe null
            invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldNotBe null
            invoke(tsh, tr("1.9", rr("4.0", null))) shouldNotBe null
            invoke(tsh, tr("10.9", rr("4.0", "10.0"), "mg/L")) shouldNotBe null
            invoke(tsh, tr("na", rr("4.0", "5.9"), "mg/L")) shouldNotBe null
        }

        with(ExtendedHighNormalRangeSuggestion(All).invoke(tsh, tr("1.8", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe normalOrSlightlyHigh(null, tsh,  10, All)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedHighNormalRangeCondition(tsh, All)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedHighNormalRangeSuggestion(Current).createEditableCondition(tsh) shouldBe EditableExtendedHighNormalRangeCondition(tsh, Current)
    }
}