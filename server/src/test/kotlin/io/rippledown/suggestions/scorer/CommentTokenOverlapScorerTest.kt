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
        //Given a context with no rule action
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(tsh))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then no candidate accumulates any overlap
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
        //Given the action's comment is "TSH is high"
        val ctx = ctxFor("TSH is high")

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then the High candidate matches both attribute and direction (2),
        //while Low matches only the attribute (1)
        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 1
    }

    /**
     * The "is" stopword must not contribute on its own — otherwise every
     * candidate would score at least 1 on a comment like "TSH is high".
     */
    @Test
    fun `stopwords do not contribute to the score`() {
        //Given a comment dominated by stopwords ("is", "the", "of")
        val ctx = ctxFor("Is the value of MCV")

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then only the surviving "mcv" token contributes to overlap; an
        //unrelated candidate gets nothing
        scorer.score(nonEdit(EpisodicCondition(mcv, High, Current))) shouldBe 1
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
        //Given comment "TSH normal range" — tokens {tsh, normal, range}
        val ctx = ctxFor("TSH normal range")
        val extendedHighNormal = EditableSuggestedCondition(
            EditableExtendedHighNormalRangeCondition(tsh, Current)
        )
        val plainHigh = nonEdit(EpisodicCondition(tsh, High, Current))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then extendedHighNormal {tsh, high, normal} ∩ comment = {tsh, normal} (2),
        //while plainHigh {tsh, high} ∩ comment = {tsh} (1)
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
        //Given comment "Glucose missing"
        val ctx = ctxFor("Glucose missing")
        val absent = nonEdit(CaseStructureCondition(IsAbsentFromCase(glucose)))
        val present = nonEdit(CaseStructureCondition(IsPresentInCase(glucose)))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then absent {glucose, absent, missing} ∩ comment = {glucose, missing} (2),
        //present {glucose, present} ∩ comment = {glucose} (1)
        scorer.score(absent) shouldBe 2
        scorer.score(present) shouldBe 1
    }

    /**
     * Direction-only matches (no attribute name in the comment) must still
     * count: clinicians often write generic phrases like "values are high".
     */
    @Test
    fun `predicate-only match still scores when attribute name is absent from the comment`() {
        //Given a generic direction-only comment with no attribute name
        val ctx = ctxFor("values are high across the board")

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then High picks up the direction word; Low gets nothing
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
        //Given a comment that mentions a quantity ("three")
        val ctx = ctxFor("at least three episodes")
        val atLeast1 = nonEdit(EpisodicCondition(tsh, High, AtLeastSignature(1)))
        val atLeast3 = nonEdit(EpisodicCondition(tsh, High, AtLeastSignature(3)))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then signature numbers contribute no tokens and the two candidates
        //score identically
        scorer.score(atLeast1) shouldBe scorer.score(atLeast3)
    }

    /**
     * `ChangeTreeToReplaceConclusion` scores against the *replacement* text,
     * not the comment being replaced. This matches the historical scorer's
     * convention: we surface signal for the comment the user is introducing.
     */
    @Test
    fun `replace action scores against replacement text, not the original`() {
        //Given a Replace action whose replacement comment is "TSH is high"
        val original = Conclusion(1, "TSH is low")
        val replacement = Conclusion(2, "TSH is high")
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToReplaceConclusion(toBeReplaced = original, replacement = replacement),
        )

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then High matches both attribute and direction from the replacement
        //text (2); Low matches only the attribute (1)
        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
        scorer.score(nonEdit(EpisodicCondition(tsh, Low, Current))) shouldBe 1
    }

    /**
     * Remove uses the comment being removed: it surfaces conditions related
     * to the comment the user is deleting, again purely informational.
     */
    @Test
    fun `remove action scores against the conclusion being removed`() {
        //Given a Remove action targeting the comment "TSH is high"
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToRemoveConclusion(Conclusion(1, "TSH is high")),
        )

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then the candidate matching that comment scores 2
        scorer.score(nonEdit(EpisodicCondition(tsh, High, Current))) shouldBe 2
    }

    /**
     * `GreaterThanOrEqualsSuggestion` exposes "high", "above", "greater" so a
     * comment like "TSH above 5" gives it a score of 3 — clearly beating a
     * plain `High` candidate that only matches "tsh" + "high".
     */
    @Test
    fun `greater-than-equals candidate picks up above and greater tokens`() {
        //Given comment "TSH above 5"
        val ctx = ctxFor("TSH above 5")
        val gte = EditableSuggestedCondition(
            EditableGreaterThanEqualsCondition(tsh, EditableValue("5.0", Type.Real), Current)
        )
        val plainHigh = nonEdit(EpisodicCondition(tsh, High, Current))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then gte {tsh, high, above, greater} ∩ comment = {tsh, above} (2),
        //plainHigh {tsh, high} ∩ comment = {tsh} (1)
        scorer.score(gte) shouldBe 2
        scorer.score(plainHigh) shouldBe 1
    }

    /**
     * Decreasing series predicates expose `decreasing`, `falling`, `trend`.
     */
    @Test
    fun `series decreasing condition matches falling synonym`() {
        //Given comment "TSH falling rapidly"
        val ctx = ctxFor("TSH falling rapidly")
        val decreasing = nonEdit(SeriesCondition(null, tsh, Decreasing))
        val increasing = nonEdit(SeriesCondition(null, tsh, Increasing))

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then decreasing {tsh, decreasing, falling, trend} ∩ comment = {tsh, falling} (2);
        //increasing {tsh, increasing, rising, trend} ∩ comment = {tsh} (1)
        scorer.score(decreasing) shouldBe 2
        scorer.score(increasing) shouldBe 1
    }

    /**
     * `IsNumeric` exposes `numeric`. Sanity check that the predicate token
     * survives even though it is not a direction word.
     */
    @Test
    fun `is-numeric scores on the word numeric`() {
        //Given comment "TSH must be numeric"
        val ctx = ctxFor("TSH must be numeric")

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then the IsNumeric candidate matches both "tsh" and "numeric"
        scorer.score(nonEdit(EpisodicCondition(tsh, IsNumeric, Current))) shouldBe 2
    }

    /**
     * `Is(value)` and `Contains(value)` tokenise the value text, so a comment
     * mentioning the value contributes overlap.
     */
    @Test
    fun `is-value condition tokenises the value`() {
        //Given comment "TSH stable since admission" and a candidate Is("stable")
        val ctx = ctxFor("TSH stable since admission")

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then both "tsh" and "stable" match
        scorer.score(nonEdit(EpisodicCondition(tsh, Is("stable"), Current))) shouldBe 2
    }

    /**
     * An empty editable contains-value should not crash and should fall back
     * to attribute-only tokens — guards against the
     * `EditableDoesNotContainCondition` / blank-value initial state.
     */
    @Test
    fun `editable contains with blank value scores on attribute name only`() {
        //Given a Contains candidate with no value yet (the editable initial state)
        val ctx = ctxFor("TSH must contain Bondi")
        val emptyContains = EditableSuggestedCondition(
            EditableContainsCondition(tsh, "", Current)
        )

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then only the attribute name contributes
        scorer.score(emptyContains) shouldBe 1
    }

    /**
     * `EditableLessThanEqualsCondition` exposes "low", "below", "less".
     */
    @Test
    fun `less-than-equals candidate picks up below token`() {
        //Given comment "TSH below threshold"
        val ctx = ctxFor("TSH below threshold")
        val lte = EditableSuggestedCondition(
            EditableLessThanEqualsCondition(tsh, EditableValue("5.0", Type.Real), Current)
        )

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then lte matches both "tsh" and "below"
        scorer.score(lte) shouldBe 2
    }

    /**
     * `EditableExtendedHighRangeCondition` exposes only the `high` direction
     * (no `normal`), per the design's token table.
     */
    @Test
    fun `extended high range candidate exposes high but not normal`() {
        //Given comment "TSH normal" — tokens {tsh, normal}
        val ctx = ctxFor("TSH normal")
        val extendedHigh = EditableSuggestedCondition(
            EditableExtendedHighRangeCondition(tsh, Current)
        )

        //When
        val scorer = CommentTokenOverlapScorer(ctx)

        //Then extendedHigh {tsh, high} ∩ comment = {tsh} (1) — "normal" is
        //deliberately not in the high-range vocabulary
        scorer.score(extendedHigh) shouldBe 1
    }
}
