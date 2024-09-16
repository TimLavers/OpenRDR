package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableExtendedLowRangeCondition
import io.rippledown.model.rule.ExtendedLowRangeSuggestion.createEditableCondition
import io.rippledown.model.rule.ExtendedLowRangeSuggestion.invoke
import io.rippledown.model.rule.ExtendedLowRangeSuggestion.rangeAndValueSuitable
import kotlin.test.Test

class ExtendedLowRangeSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun suggestion() {
        invoke(tsh, null) shouldBe null
        invoke(tsh, tr("whatever")) shouldBe null
        invoke(tsh, tr("1.9")) shouldBe null
        invoke(tsh, tr("1.9", rr(null, "10.0"))) shouldBe null
        invoke(tsh, tr("1.9", rr("4.0", "10.0"))) shouldNotBe null
        invoke(tsh, tr("1.9", rr("4.0", null))) shouldNotBe null
        invoke(tsh, tr("1.9", rr("4.0", null), "mg/L")) shouldNotBe null
        invoke(tsh, tr("na", rr("4.0", null))) shouldBe null
        invoke(tsh, tr("na", rr("4.0", null), "mg/L")) shouldBe null

        with(invoke(tsh, tr("1.2", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe slightlyLow(null, tsh,  10)
            isEditable() shouldBe true
            editableCondition() shouldBe EditableExtendedLowRangeCondition(tsh)
        }
    }

    @Test
    fun createEditableConditionTest() {
        createEditableCondition(tsh) shouldBe EditableExtendedLowRangeCondition(tsh)
    }

    @Test
    fun rangeAndValueSuitableTest() {
        rangeAndValueSuitable(rr(null, "1.0"), v("3.4")) shouldBe false
        rangeAndValueSuitable(rr("0.5", "1.0"), v("blah")) shouldBe false
        rangeAndValueSuitable(rr("0.5", "1.0"), v("0.1")) shouldBe true
        rangeAndValueSuitable(rr("0.5", "1.0"), v("0.6")) shouldBe false
        rangeAndValueSuitable(rr("0.5", "1.4"), v("0.6")) shouldBe false
        rangeAndValueSuitable(rr("0.5", null), v("0.45")) shouldBe true
    }
}