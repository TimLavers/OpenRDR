package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedLowNormalRangeCondition
import io.rippledown.model.condition.episodic.signature.All
import io.rippledown.model.condition.episodic.signature.Current
import kotlin.test.Test

class ExtendedLowNormalRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        with(ExtendedLowNormalRangeSuggestion(Current)) {
            invoke(tsh, null) shouldBe null
            invoke(tsh, tr("whatever")) shouldBe null
            invoke(tsh, tr("1.9")) shouldBe null
            invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldNotBe null
            invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldNotBe null
            invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldNotBe null
            invoke(tsh, tr("1.9", rr("4.0", null))) shouldNotBe null
            invoke(tsh, tr("1.9", rr("4.0", null), "mg/L")) shouldNotBe null
            invoke(tsh, tr("na", rr("4.0", null))) shouldBe null
            invoke(tsh, tr("na", rr("4.0", null), "mg/L")) shouldBe null

        }

        with(ExtendedLowNormalRangeSuggestion(All).invoke(tsh, tr("1.2", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe normalOrSlightlyLow(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedLowNormalRangeCondition(tsh, All)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedLowNormalRangeSuggestion(All).createEditableCondition(tsh) shouldBe EditableExtendedLowNormalRangeCondition(tsh, All)
    }
}