package io.rippledown.model.rule

import io.kotest.matchers.shouldBe
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.No
import io.rippledown.model.condition.isCondition
import io.rippledown.model.condition.tr
import kotlin.test.Test

class IsSuggestionTest: ConditionFactoryTestBase() {
    @Test
    fun createFor() {
        IsSuggestion(Current).invoke(tsh, null) shouldBe null
        with(IsSuggestion(Current).invoke(tsh, tr("whatever"))!!) {
            isEditable() shouldBe false
            editableCondition() shouldBe null
            initialSuggestion() shouldBe isCondition(null, tsh, "whatever")
        }
        with(IsSuggestion(No).invoke(tsh, tr("whatever"))!!) {
            isEditable() shouldBe false
            editableCondition() shouldBe null
            initialSuggestion() shouldBe EpisodicCondition(tsh, Is("whatever"), No)
        }
    }
}