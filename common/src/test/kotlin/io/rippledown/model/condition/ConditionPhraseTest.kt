package io.rippledown.model.condition

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.Attribute
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.utils.serializeDeserialize
import org.junit.jupiter.api.Test

class ConditionPhraseTest {
    @Test
    fun `replacing a phrase preserves each predicate and id without mutating the original`() {
        // Given each stored condition kind
        val attribute = Attribute(1, "Glucose")
        val conditions = listOf(
            isHigh(1, attribute, "original"),
            SeriesCondition(2, attribute, Increasing, "original"),
            CaseStructureCondition(3, IsPresentInCase(attribute), "original")
        )

        conditions.forEach { original ->
            // When
            val renamed = original.withUserExpression("new phrase")

            // Then
            renamed.userExpression() shouldBe "new phrase"
            renamed.id shouldBe original.id
            renamed.asText() shouldBe original.asText()
            renamed.sameAs(original) shouldBe true
            original.userExpression() shouldBe "original"
            serializeDeserialize<Condition>(renamed) shouldBe renamed
        }
    }

    @Test
    fun `synthetic conditions keep their identity and have no phrase`() {
        // Given
        val conditions = listOf(True, And(True, True))

        conditions.forEach { condition ->
            // When
            val renamed = condition.withUserExpression("a phrase")

            // Then
            renamed shouldBeSameInstanceAs condition
            renamed.userExpression() shouldBe ""
        }
    }
}
