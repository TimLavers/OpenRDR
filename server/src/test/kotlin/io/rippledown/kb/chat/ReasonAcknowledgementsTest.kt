package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.condition.greaterThanOrEqualTo
import org.junit.jupiter.api.Test

class ReasonAcknowledgementsTest {
    private val condition = greaterThanOrEqualTo(7, Attribute(1, "Glucose"), 11.0)
        .copy(userExpression = "elevated glucose")
    private val acknowledgements = ReasonAcknowledgements()

    @Test
    fun `records the authoritative message and keeps snapshots independent of later turns`() {
        // Given
        acknowledgements.record(ConditionParsingResult(condition, expression = "raised glucose"))

        // When
        val snapshot = acknowledgements.snapshot()
        acknowledgements.clear()

        // Then
        snapshot shouldBe mapOf(
            condition.asText() to
                    "Added your reason '${condition.asText()}' (you previously called this 'elevated glucose')."
        )
        acknowledgements.snapshot() shouldBe emptyMap()
    }

    @Test
    fun `failed transformations and bare Ok results do not produce acknowledgements`() {
        // Given
        val formalOnly = condition.copy(userExpression = condition.asText())

        // When
        acknowledgements.record(ConditionParsingResult(null, "Cannot parse that reason"))
        acknowledgements.record(ConditionParsingResult(formalOnly, expression = formalOnly.asText()))

        // Then
        acknowledgements.snapshot() shouldBe emptyMap()
    }
}
