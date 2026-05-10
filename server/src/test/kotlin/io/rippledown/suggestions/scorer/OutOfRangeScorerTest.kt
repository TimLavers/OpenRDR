package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Is
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.series.Increasing
import io.rippledown.model.condition.structural.IsSingleEpisodeCase
import io.rippledown.model.rule.makeCase
import io.rippledown.suggestions.SuggestionContext
import kotlin.test.Test

class OutOfRangeScorerTest {

    private val ast = Attribute(1, "AST")
    private val alt = Attribute(2, "ALT")
    private val noRange = Attribute(3, "Comment")

    private fun nonEdit(condition: io.rippledown.model.condition.Condition): SuggestedCondition =
        NonEditableSuggestedCondition(condition)

    /**
     * AST is high (36 with reference 0..30); ALT is normal (29 with the
     * same range). Conditions on AST should score 1; conditions on ALT
     * (and on attributes whose value is normal or non-numeric) score 0.
     */
    @Test
    fun `attribute whose latest value is high scores 1, in-range attribute scores 0`() {
        //Given a case where AST is high and ALT is normal
        val sessionCase = makeCase(
            ast to tr("36", rr("0", "30")),
            alt to tr("29", rr("0", "30")),
        )
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(ast, alt))

        //When
        val scorer = OutOfRangeScorer(ctx)

        //Then
        scorer.score(nonEdit(EpisodicCondition(ast, Is("36"), Current))) shouldBe 1
        scorer.score(nonEdit(EpisodicCondition(alt, Is("29"), Current))) shouldBe 0
    }

    /**
     * The score is keyed off the attribute's reference-range membership,
     * not the condition's predicate. Even an irrelevant predicate
     * (e.g. `is high` for an attribute that is actually low) inherits
     * the attribute-level out-of-range bonus.
     */
    @Test
    fun `score is per-attribute and ignores the candidate's predicate`() {
        //Given AST high
        val sessionCase = makeCase(ast to tr("36", rr("0", "30")))
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(ast))

        //When
        val scorer = OutOfRangeScorer(ctx)

        //Then any predicate referencing AST inherits the out-of-range bonus
        scorer.score(nonEdit(EpisodicCondition(ast, High, Current))) shouldBe 1
        scorer.score(nonEdit(EpisodicCondition(ast, Is("36"), Current))) shouldBe 1
        scorer.score(nonEdit(SeriesCondition(attribute = ast, seriesPredicate = Increasing))) shouldBe 1
    }

    /**
     * Attributes without a reference range cannot be classified as low /
     * normal / high and therefore score 0 — nothing distinguishing about
     * them at this stage.
     */
    @Test
    fun `attribute without reference range scores 0`() {
        //Given an attribute with a free-text value and no reference range
        val sessionCase = makeCase(noRange to tr("clinical note"))
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(noRange))

        //When
        val scorer = OutOfRangeScorer(ctx)

        //Then no out-of-range bonus is applied
        scorer.score(nonEdit(EpisodicCondition(noRange, Is("clinical note"), Current))) shouldBe 0
    }

    /**
     * Conditions with no meaningful attribute (case-structure predicates
     * such as `is single episode`) bypass the scorer and score 0.
     */
    @Test
    fun `conditions with no attribute score 0`() {
        //Given any case
        val sessionCase = makeCase(ast to tr("36", rr("0", "30")))
        val ctx = SuggestionContext(sessionCase = sessionCase, attributes = setOf(ast))

        //When
        val scorer = OutOfRangeScorer(ctx)

        //Then case-structure predicates contribute no signal
        scorer.score(nonEdit(CaseStructureCondition(IsSingleEpisodeCase))) shouldBe 0
    }
}
