package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedHighRangeCondition
import kotlin.test.Test

class ExtendedHighRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        ExtendedHighRangeSuggestion.invoke(tsh, null) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("whatever")) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("1.9")) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("6.9", rr("4.0", "10.0"))) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("1.9", rr("4.0", null))) shouldBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("10.9", rr("4.0", "10.0"), "mg/L")) shouldNotBe null
        ExtendedHighRangeSuggestion.invoke(tsh, tr("na", rr("4.0", "5.9"), "mg/L")) shouldBe null

        with(ExtendedHighRangeSuggestion.invoke(tsh, tr("5.8", rr("1.5", "5.0")))!!) {
            initialSuggestion() shouldBe slightlyHigh(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedHighRangeCondition(tsh)
        }
    }

    @Test
    fun createEditableConditionTest() {
        ExtendedHighRangeSuggestion.createEditableCondition(tsh) shouldBe EditableExtendedHighRangeCondition(tsh)
    }

    @Test
    fun rangeAndValueSuitableTest() {
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr(null, "1.0"), v("3.4")) shouldBe true
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.1")) shouldBe false
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.0"), v("0.6")) shouldBe false
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", "1.4"), v("1.6")) shouldBe true
        ExtendedHighRangeSuggestion.rangeAndValueSuitable(rr("0.5", null), v("0.45")) shouldBe false
    }
}