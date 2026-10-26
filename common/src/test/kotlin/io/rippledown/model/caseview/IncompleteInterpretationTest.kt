package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IncompleteInterpretationTest {

    @Test
    fun `a failed evaluation names the case, the attribute and the reason`() {
        // Given
        val incomplete = IncompleteInterpretation(
            caseId = 42,
            category = IncompleteCategory.EVALUATION_FAILED,
            attributeName = "hasDiabetes",
            reason = "Gemini unavailable"
        )

        // When
        val restored = serializeDeserialize(incomplete)

        // Then
        restored shouldBe incomplete
    }

    @Test
    fun `an abandoned interpretation has no attribute`() {
        // Given
        val incomplete = IncompleteInterpretation(
            caseId = 42,
            category = IncompleteCategory.ABANDONED,
            attributeName = null,
            reason = "The knowledge base kept changing."
        )

        // When
        val restored = serializeDeserialize(incomplete)

        // Then
        restored shouldBe incomplete
        restored.attributeName shouldBe null
    }
}
