package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.isNumeric
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

class NonEditableConditionSuggesterTest: ConditionFactoryTestBase()  {
    @Test
    fun createFor() {
        NonEditableConditionSuggester(High, Current).invoke(tsh, null) shouldBe null
        NonEditableConditionSuggester(High, Current).invoke(tsh, tr("whatever")) shouldNotBe null
        NonEditableConditionSuggester(High, Current).invoke(tsh, tr("2.3")) shouldNotBe null
        NonEditableConditionSuggester(High, Current).invoke(tsh, TestResult("2.3", null, "m/L")) shouldNotBe null
        with(NonEditableConditionSuggester(High, Current).invoke(tsh, TestResult("2.3", rr("1.2", null), "m/L"))!!) {
            isEditable() shouldBe false
            initialSuggestion() shouldBe EpisodicCondition(tsh, High, Current)
            editableCondition() shouldBe null
        }
        with(NonEditableConditionSuggester(Normal, Current).invoke(tsh, TestResult("2.3", rr("1.2", "2.0"), "m/L"))!!) {
            isEditable() shouldBe false
            initialSuggestion() shouldBe EpisodicCondition(tsh, Normal, Current)
            editableCondition() shouldBe null
        }
        with(NonEditableConditionSuggester(Low, Current).invoke(tsh, TestResult("", rr(null,"2.0"), "m/L"))!!) {
            initialSuggestion() shouldBe EpisodicCondition(tsh, Low, Current)
        }
    }

    @Test
    fun isNumeric() {
        NonEditableConditionSuggester(IsNumeric, Current).invoke(tsh, null) shouldBe null
        NonEditableConditionSuggester(IsNumeric, Current).invoke(tsh, tr("whatever")) shouldNotBe null

        with(NonEditableConditionSuggester(IsNumeric, Current).invoke(tsh, tr("1.2", rr("1.5", null)))!!) {
            initialSuggestion() shouldBe isNumeric(tsh)
            isEditable() shouldBe false
            editableCondition() shouldBe null
        }
    }
}