package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedLowNormalRangeCondition
import kotlin.test.Test

class ExtendedLowNormalRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        ExtendedLowNormalRangeSuggestion.invoke(tsh, null) shouldBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("whatever")) shouldBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.9")) shouldBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldNotBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldNotBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldNotBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", null))) shouldNotBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", null), "mg/L")) shouldNotBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("na", rr("4.0", null))) shouldBe null
        ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("na", rr("4.0", null), "mg/L")) shouldBe null

        with(ExtendedLowNormalRangeSuggestion.invoke(tsh, tr("1.2", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe normalOrSlightlyLow(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedLowNormalRangeCondition(tsh)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedLowNormalRangeSuggestion.createEditableCondition(tsh) shouldBe EditableExtendedLowNormalRangeCondition(tsh)
    }

    @Test
    fun rangeAndValueSuitableTest() {
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr(null, "1.0"), v("3.4")) shouldBe false
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.1")) shouldBe true
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.6")) shouldBe true
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.4"), v("1.6")) shouldBe false
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", null), v("0.45")) shouldBe true
        ExtendedLowNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", null), v("0.55")) shouldBe true
    }
}