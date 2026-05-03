package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.CaseStructureCondition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.SeriesCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.predicate.IsNumeric
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Decreasing
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsAbsentFromCase
import io.rippledown.model.condition.structural.IsPresentInCase
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.ChangeTreeToRemoveConclusion
import io.rippledown.model.rule.ChangeTreeToReplaceConclusion
import io.rippledown.model.rule.case
import io.rippledown.suggestions.SuggestionContext
import kotlin.test.Test
import io.rippledown.model.condition.episodic.signature.AtLeast as AtLeastSignature

class CommentTokenOverlapScorerTest {

    private val tsh = Attribute(10, "TSH")
    private val mcv = Attribute(11, "MCV")
    private val glucose = Attribute(12, "Glucose")

    private val sessionCase = case(tsh to "5.0")

    private fun nonEdit(condition: io.rippledown.model.condition.Condition): SuggestedCondition =
        NonEditableSuggestedCondition(condition)

    private fun ctxFor(commentText: String) = SuggestionContext(
        sessionCase = sessionCase,
        attributes = setOf(tsh, mcv, glucose),
        action = ChangeTreeToAddConclusion(Conclusion(1, commentText)),
    )

    /**
     * Null action means there is no comment to score against; every candidate
     * scores 0 and the ranker degrades to historical / alphabetic order.
     */
    @Test
    fun `null action scores 0 for every candidate`() {
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(tsh))
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 0
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 0
    }

    /**
     * "TSH is high" matches both the attribute name and the direction, scoring
     * 2 for the High candidate and 1 (attribute only) for Low. This is the
     * canonical disambiguator the scorer is designed for.
     */
    @Test
    fun `attribute name plus direction outranks attribute name alone`() {
        val ctx = ctxFor("TSH is high")
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 1
    }

    /**
     * The "is" stopword must not contribute on its own — otherwise every
     * candidate would score at least 1 on a comment like "TSH is high".
     */
    @Test
    fun `stopwords do not contribute to the score`() {
        val ctx = ctxFor("Is the value of MCV")
        val scorer = CommentTokenOverlapScorer(ctx)

        //Only "value" and "mcv" survive tokenisation; only "mcv" matches a
        //condition token.
        scorer.score(nonEdit(EpisodicCondition(mcv, High, Current))) shouldBe 1
        //Other candidates with no matching attribute / direction get 0.
        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 0
    }

    /**
     * A candidate built from `ExtendedHighNormalRangeSuggestion` exposes both
     * `high` and `normal` direction tokens via its `EpisodicCondition`-shaped
     * `initialSuggestion`, so on "TSH is in the normal range" it must beat a
     * plain `High` candidate (which only contributes "high").
     */
    @Test
    fun `extended high-normal range candidate beats plain high on a normal-leaning comment`() {
        val ctx = ctxFor("TSH normal range")
        val scorer = CommentTokenOverlapScorer(ctx)

        val extendedHighNormal = EditableSuggestedCondition(
            EditableExtendedHighNormalRangeCondition(tsh, Current)
        )
        val plainHigh = nonEdit(EpisodicCondition(tsh, High, Current))

        //"tsh", "normal", "range" survive tokenisation.
        //extendedHighNormal contributes {tsh, high, normal} → overlap = {tsh, normal} = 2
        //plainHigh contributes        {tsh, high}          → overlap = {tsh}        = 1
        scorer.score(extendedHighNormal) shouldBe 2
        scorer.score(plainHigh) shouldBe 1
    }

    /**
     * "Glucose missing" tokens map directly onto the
     * `IsAbsentFromCase(Glucose)` candidate's tokens via the
     * `absent`/`missing` synonym pair.
     */
    @Test
    fun `absent-from-case scores on missing synonym`() {
        val ctx = ctxFor("Glucose missing")
        val scorer = CommentTokenOverlapScorer(ctx)

        val absent = nonEdit(CaseStructureCondition(IsAbsentFromCase(glucose)))
        val present = nonEdit(CaseStructureCondition(IsPresentInCase(glucose)))

        //absent contributes {glucose, absent, missing} → overlap = {glucose, missing} = 2
        //present contributes {glucose, present}        → overlap = {glucose}          = 1
        scorer.score(absent) shouldBe 2
        scorer.score(present) shouldBe 1
    }

    /**
     * Direction-only matches (no attribute name in the comment) must still
     * count: clinicians often write generic phrases like "values are high".
     */
    @Test
    fun `predicate-only match still scores when attribute name is absent from the comment`() {
        val ctx = ctxFor("values are high across the board")
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 1
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 0
    }

    /**
     * Signature tokens (`AtLeast(n)`, etc.) must be ignored — including them
     * would let "at least three" boost `AtLeast(3)` over `AtLeast(1)` without
     * any semantic justification.
     */
    @Test
    fun `signature numbers do not affect the score`() {
        val ctx = ctxFor("at least three episodes")
        val scorer = CommentTokenOverlapScorer(ctx)

        val atLeast1 = nonEdit(EpisodicCondition(tsh, High, AtLeastSignature(1)))
        val atLeast3 = nonEdit(EpisodicCondition(tsh, High, AtLeastSignature(3)))

        //Both candidates carry identical condition tokens (signature ignored)
        scorer.score(atLeast1) shouldBe scorer.score(atLeast3)
    }

    /**
     * `ChangeTreeToReplaceConclusion` scores against the *replacement* text,
     * not the comment being replaced. This matches the historical scorer's
     * convention: we surface signal for the comment the user is introducing.
     */
    @Test
    fun `replace action scores against replacement text, not the original`() {
        val original = Conclusion(1, "TSH is low")
        val replacement = Conclusion(2, "TSH is high")
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToReplaceConclusion(toBeReplaced = original, replacement = replacement),
        )
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 1
    }

    /**
     * Remove uses the comment being removed: it surfaces conditions related
     * to the comment the user is deleting, again purely informational.
     */
    @Test
    fun `remove action scores against the conclusion being removed`() {
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToRemoveConclusion(Conclusion(1, "TSH is high")),
        )
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
    }

    /**
     * `GreaterThanOrEqualsSuggestion` exposes "high", "above", "greater" so a
     * comment like "TSH above 5" gives it a score of 3 — clearly beating a
     * plain `High` candidate that only matches "tsh" + "high".
     */
    @Test
    fun `greater-than-equals candidate picks up above and greater tokens`() {
        val ctx = ctxFor("TSH above 5")
        val scorer = CommentTokenOverlapScorer(ctx)

        val gte = EditableSuggestedCondition(
            EditableGreaterThanEqualsCondition(tsh, EditableValue("5.0", Type.Real), Current)
        )
        val plainHigh = nonEdit(EpisodicCondition(tsh, High, Current))

        //gte contributes {tsh, high, above, greater} → overlap = {tsh, above} = 2
        //plainHigh contributes {tsh, high}           → overlap = {tsh}        = 1
        scorer.score(gte) shouldBe 2
        scorer.score(plainHigh) shouldBe 1
    }

    /**
     * Decreasing series predicates expose `decreasing`, `falling`, `trend`.
     */
    @Test
    fun `series decreasing condition matches falling synonym`() {
        val ctx = ctxFor("TSH falling rapidly")
        val scorer = CommentTokenOverlapScorer(ctx)

        val decreasing = nonEdit(SeriesCondition(null, tsh, Decreasing))
        val increasing = nonEdit(SeriesCondition(null, tsh, Increasing))

        //decreasing contributes {tsh, decreasing, falling, trend} → overlap = {tsh, falling} = 2
        scorer.score(decreasing) shouldBe 2
        //increasing contributes {tsh, increasing, rising, trend} → overlap = {tsh}           = 1
        scorer.score(increasing) shouldBe 1
    }

    /**
     * `IsNumeric` exposes `numeric`. Sanity check that the predicate token
     * survives even though it is not a direction word.
     */
    @Test
    fun `is-numeric scores on the word numeric`() {
        val ctx = ctxFor("TSH must be numeric")
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, IsNumeric, Current))) shouldBe 2
    }

    /**
     * `Is(value)` and `Contains(value)` tokenise the value text, so a comment
     * mentioning the value contributes overlap.
     */
    @Test
    fun `is-value condition tokenises the value`() {
        val ctx = ctxFor("TSH stable since admission")
        val scorer = CommentTokenOverlapScorer(ctx)

        scorer.score(nonEdit(EpisodicCondition(tsh, Is("stable"), Current))) shouldBe 2
    }

    /**
     * An empty editable contains-value should not crash and should fall back
     * to attribute-only tokens — guards against the
     * `EditableDoesNotContainCondition` / blank-value initial state.
     */
    @Test
    fun `editable contains with blank value scores on attribute name only`() {
        val ctx = ctxFor("TSH must contain Bondi")
        val scorer = CommentTokenOverlapScorer(ctx)

        val emptyContains = EditableSuggestedCondition(
            EditableContainsCondition(tsh, "", Current)
        )
        scorer.score(emptyContains) shouldBe 1
    }

    /**
     * `EditableLessThanEqualsCondition` exposes "low", "below", "less".
     */
    @Test
    fun `less-than-equals candidate picks up below token`() {
        val ctx = ctxFor("TSH below threshold")
        val scorer = CommentTokenOverlapScorer(ctx)

        val lte = EditableSuggestedCondition(
            EditableLessThanEqualsCondition(tsh, EditableValue("5.0", Type.Real), Current)
        )
        scorer.score(lte) shouldBe 2 // {tsh, below}
    }

    /**
     * `EditableExtendedHighRangeCondition` exposes only the `high` direction
     * (no `normal`), per the design's token table.
     */
    @Test
    fun `extended high range candidate exposes high but not normal`() {
        val ctx = ctxFor("TSH normal")
        val scorer = CommentTokenOverlapScorer(ctx)

        val extendedHigh = EditableSuggestedCondition(
            EditableExtendedHighRangeCondition(tsh, Current)
        )
        //extendedHigh contributes {tsh, high} → overlap with {tsh, normal} = {tsh} = 1
        scorer.score(extendedHigh) shouldBe 1
    }
}
