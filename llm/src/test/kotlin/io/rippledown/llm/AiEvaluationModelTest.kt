package io.rippledown.llm

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AiEvaluationModelTest {
    @Test
    fun `evaluation defaults to the model defined in Gemini`() {
        // Given no explicit evaluation model
        val override: String? = null

        // When choosing the model for a new definition
        val model = aiEvaluationModel(override)

        // Then the default follows the shared Gemini setting
        model shouldBe GEMINI_MODEL
    }

    @Test
    fun `an explicit evaluation model overrides the Gemini default`() {
        // Given an explicitly configured evaluation model
        val override = "explicit-evaluation-model"

        // When choosing the model for a new definition
        val model = aiEvaluationModel(override)

        // Then that choice is preserved
        model shouldBe override
    }
}
