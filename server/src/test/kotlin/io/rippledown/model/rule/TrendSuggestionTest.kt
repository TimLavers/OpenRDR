package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.decreasing
import io.rippledown.model.condition.increasing
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.tr
import kotlin.test.Test

class TrendSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        TrendSuggestion(Increasing).invoke(notes, null) shouldBe null
        TrendSuggestion(Increasing).invoke(notes, tr("blah")) shouldBe null
        with(TrendSuggestion(Increasing).invoke(notes, tr("123"))!!) {
            isEditable() shouldBe false
            editableCondition() shouldBe null
            initialSuggestion() shouldBe increasing(notes)
        }
        with(TrendSuggestion(Decreasing).invoke(notes, tr("123"))!!) {
            isEditable() shouldBe false
            editableCondition() shouldBe null
            initialSuggestion() shouldBe decreasing(notes)
        }
    }
}