package io.rippledown.model.rule.evaluation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiEvaluationProfile
import kotlin.test.Test

internal class PreparedEvaluationsTest {
    private val notes = Attribute(1, "clinical_notes")
    private val profile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    private val hasDiabetes = AiBoolean(
        sourceAttribute = notes,
        instructions = "True if the notes affirm diabetes.",
        revision = "rev-1",
        evaluationProfile = profile
    )
    private val request = AiEvaluationRequest(hasDiabetes, "T2 Diab")
    private val success = AiEvaluationResult.Success(true, "T2 Diab", 1L, "gemini-2.5-flash")

    @Test
    fun `a new cache has no results`() {
        // Given
        val prepared = PreparedEvaluations()

        // When
        val context = prepared.context()

        // Then
        context.resultFor(request).shouldBeNull()
    }

    @Test
    fun `an added result is read by later contexts`() {
        // Given
        val prepared = PreparedEvaluations()

        // When
        prepared.add(request, success)

        // Then
        prepared.context().resultFor(request) shouldBe success
    }

    @Test
    fun `a context taken before a result was added does not see it`() {
        // Given a context taken for a run in progress
        val prepared = PreparedEvaluations()
        val before = prepared.context()

        // When a result arrives
        prepared.add(request, success)

        // Then the run's context is unchanged, so every pass of it reads the same results
        before.resultFor(request).shouldBeNull()
        prepared.context().resultFor(request) shouldBe success
    }

    @Test
    fun `a result for a different input is a different entry`() {
        // Given
        val prepared = PreparedEvaluations()
        prepared.add(request, success)

        // When
        val other = AiEvaluationRequest(hasDiabetes, "No history")

        // Then
        prepared.context().resultFor(other).shouldBeNull()
    }
}
