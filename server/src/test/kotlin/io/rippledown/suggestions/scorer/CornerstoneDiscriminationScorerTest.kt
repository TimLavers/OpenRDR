package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.case
import io.rippledown.suggestions.SuggestionContext
import kotlin.test.Test

class CornerstoneDiscriminationScorerTest {

    private val tsh = Attribute(10, "TSH")

    //Session case has TSH high (5.0). Cornerstone construction below uses
    //varying TSH values to control which conditions discriminate.
    private val sessionCase = case(tsh to "5.0")

    private fun nonEdit(condition: io.rippledown.model.condition.Condition): SuggestedCondition =
        NonEditableSuggestedCondition(condition)

    private fun ctxFor(vararg cornerstoneValues: String) = SuggestionContext(
        sessionCase = sessionCase,
        attributes = setOf(tsh),
        cornerstones = cornerstoneValues.mapIndexed { i, v -> case(tsh to v, name = "CC$i") },
    )

    /**
     * No cornerstones means no discrimination signal. Every candidate scores
     * 0 and the ranker degrades to historical / comment / alphabetic.
     */
    @Test
    fun `empty cornerstones list scores 0 for every candidate`() {
        //Given a context with no cornerstones
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(tsh))

        //When
        val scorer = CornerstoneDiscriminationScorer(ctx)

        //Then candidates contribute no discrimination signal
        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 0
    }

    /**
     * `Is "5.0"` matches the session case but excludes both cornerstones —
     * fully discriminating, score = number of cornerstones.
     */
    @Test
    fun `score equals the number of cornerstones the candidate excludes`() {
        //Given two cornerstones (TSH=1.0, TSH=2.0), both differing from the
        //session's TSH=5.0
        val ctx = ctxFor("1.0", "2.0")
        val sessionValueIs = nonEdit(
            EpisodicCondition(tsh, io.rippledown.model.condition.episodic.predicate.Is("5.0"), Current)
        )

        //When
        val scorer = CornerstoneDiscriminationScorer(ctx)

        //Then the fully discriminating candidate excludes both cornerstones
        scorer.score(sessionValueIs) shouldBe 2
    }

    /**
     * A condition that holds for some cornerstones and not others scores the
     * count of cornerstones it excludes — the discriminating ones.
     */
    @Test
    fun `partial match scores only the excluded cornerstones`() {
        //Given three cornerstones — two sharing the session's TSH=5.0,
        //one differing (TSH=1.0)
        val ctx = ctxFor("5.0", "5.0", "1.0")
        val sessionValueIs = nonEdit(
            EpisodicCondition(tsh, io.rippledown.model.condition.episodic.predicate.Is("5.0"), Current)
        )

        //When
        val scorer = CornerstoneDiscriminationScorer(ctx)

        //Then only the differing cornerstone is excluded
        scorer.score(sessionValueIs) shouldBe 1
    }

    /**
     * A condition that holds for every cornerstone is non-discriminating.
     * Score = 0 — the ranker will prefer a more selective alternative when
     * one exists.
     */
    @Test
    fun `condition holding for every cornerstone scores 0`() {
        //Given two cornerstones that share the session's TSH=5.0
        val ctx = ctxFor("5.0", "5.0")
        val sessionValueIs = nonEdit(
            EpisodicCondition(tsh, io.rippledown.model.condition.episodic.predicate.Is("5.0"), Current)
        )

        //When
        val scorer = CornerstoneDiscriminationScorer(ctx)

        //Then the candidate excludes none of them and scores 0
        scorer.score(sessionValueIs) shouldBe 0
    }

    /**
     * Editable suggestions are scored against their `initialSuggestion()` —
     * the concrete condition the user would commit if they accepted the
     * suggestion as-is. The default editable value is what the generator
     * filled in from the session case.
     */
    @Test
    fun `editable suggestion is scored against its initial condition`() {
        //Given an editable "TSH ≥ 5.0" candidate and two cornerstones at
        //TSH=1.0 (fails the predicate) and TSH=10.0 (satisfies it)
        val ctx = ctxFor("1.0", "10.0")
        val gte = EditableSuggestedCondition(
            EditableGreaterThanEqualsCondition(tsh, EditableValue("5.0", Type.Real), Current)
        )

        //When
        val scorer = CornerstoneDiscriminationScorer(ctx)

        //Then only the 1.0 cornerstone is excluded (the editable's initial
        //condition is what gets evaluated)
        scorer.score(gte) shouldBe 1
    }
}
