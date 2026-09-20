package io.rippledown.model.rule

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionText
import io.rippledown.model.condition.isHigh
import org.junit.jupiter.api.Test

class RuleConditionReplacementTest {
    @Test
    fun `replacing a condition refreshes summaries without changing earlier copies`() {
        // Given
        val original = isHigh(1, Attribute(1, "Glucose"), "elevated")
        val rule = Rule(1, conditions = setOf(original))
        val copy = rule.copy()
        val renamed = original.withUserExpression("raised")

        // When
        rule.replaceCondition(renamed)

        // Then
        rule.conditions shouldBe setOf(renamed)
        rule.summary().conditionsFromRoot shouldBe listOf(ConditionText(original.asText(), "raised"))
        copy.conditions shouldBe setOf(original)
        rule.id shouldBe 1
    }

    @Test
    fun `unrelated condition ids leave the rule unchanged`() {
        // Given
        val original = isHigh(1, Attribute(1, "Glucose"))
        val rule = Rule(1, conditions = setOf(original))

        // When
        rule.replaceCondition(isHigh(2, Attribute(2, "TSH"), "raised"))

        // Then
        rule.conditions shouldBe setOf(original)
    }

    @Test
    fun `an unstored replacement is refused`() {
        // Given
        val rule = Rule(1)

        // When / Then
        shouldThrow<IllegalArgumentException> { rule.replaceCondition(isHigh(null, Attribute(1, "Glucose"))) }
        rule.conditions shouldBe emptySet()
    }
}
