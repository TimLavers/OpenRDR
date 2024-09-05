package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.TestResult
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import kotlin.test.Test

class RangeConditionSuggesterTest: ConditionFactoryTestBase()  {
    @Test
    fun createFor() {
        RangeConditionSuggester(High).invoke(tsh, null) shouldBe null
        RangeConditionSuggester(High).invoke(tsh, tr("whatever")) shouldBe null
        RangeConditionSuggester(High).invoke(tsh, tr("2.3")) shouldBe null
        RangeConditionSuggester(High).invoke(tsh, TestResult("2.3", null, "m/L")) shouldBe null
        with(RangeConditionSuggester(High).invoke(tsh, TestResult("2.3", rr("1.2", null), "m/L"))!!) {
            isEditable() shouldBe false
            initialSuggestion() shouldBe EpisodicCondition(tsh, High, Current)
            editableCondition() shouldBe null
        }
        with(RangeConditionSuggester(Normal).invoke(tsh, TestResult("2.3", rr("1.2", "2.0"), "m/L"))!!) {
            isEditable() shouldBe false
            initialSuggestion() shouldBe EpisodicCondition(tsh, Normal, Current)
            editableCondition() shouldBe null
        }
        with(RangeConditionSuggester(Low).invoke(tsh, TestResult("", rr(null,"2.0"), "m/L"))!!) {
            initialSuggestion() shouldBe EpisodicCondition(tsh, Low, Current)
        }
    }
}