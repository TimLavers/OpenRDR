package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedHighNormalRangeCondition
import kotlin.test.Test

class ExtendedHighNormalRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        ExtendedHighNormalRangeSuggestion.invoke(tsh, null) shouldBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("whatever")) shouldBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("1.9")) shouldBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldNotBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldNotBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", null))) shouldBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("10.9", rr("4.0", "10.0"), "mg/L")) shouldNotBe null
        ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("na", rr("4.0", "5.9"), "mg/L")) shouldBe null

        with(ExtendedHighNormalRangeSuggestion.invoke(tsh, tr("1.8", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe normalOrSlightlyHigh(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedHighNormalRangeCondition(tsh)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedHighNormalRangeSuggestion.createEditableCondition(tsh) shouldBe EditableExtendedHighNormalRangeCondition(tsh)
    }

    @Test
    fun rangeAndValueSuitableTest() {
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr(null, "1.0"), v("3.4")) shouldBe true
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.1")) shouldBe false
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.6")) shouldBe true
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.4"), v("1.6")) shouldBe true
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", null), v("0.45")) shouldBe false
        ExtendedHighNormalRangeSuggestion.rangeAndValueSuitable(rr("0.5", null), v("0.55")) shouldBe true
    }
}