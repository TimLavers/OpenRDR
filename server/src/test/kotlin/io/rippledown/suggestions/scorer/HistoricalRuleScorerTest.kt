package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.*
import io.rippledown.suggestions.SuggestionContext
import kotlin.test.Test

class HistoricalRuleScorerTest {

    private val tsh = Attribute(10, "TSH")
    private val mcv = Attribute(11, "MCV")

    private val goToBondi = Conclusion(100, "Go to Bondi.")
    private val otherConclusion = Conclusion(101, "Go to Manly.")

    private val tshHigh = EpisodicCondition(tsh, High, Current)
    private val tshLow = EpisodicCondition(tsh, Low, Current)
    private val mcvHigh = EpisodicCondition(mcv, High, Current)

    private val sessionCase = case(tsh to "5.0")

    private fun suggestionFor(condition: EpisodicCondition): SuggestedCondition =
        NonEditableSuggestedCondition(condition)

    private fun ruleTreeWith(vararg rules: Rule): RuleTree {
        val root = Rule(0)
        rules.forEach { root.addChild(it) }
        return RuleTree(root)
    }

    /**
     * No action on the context means no target conclusion — the scorer has
     * nothing to anchor on, so every candidate scores 0.
     */
    @Test
    fun `returns 0 for every candidate when action is null`() {
        //Given a tree with a rule using tshHigh for Go to Bondi but no active action
        val rule = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = null,
            ruleTree = ruleTreeWith(rule),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then
        score shouldBe 0
    }

    /**
     * Cold start: no rule in the tree has the target conclusion, so every
     * candidate scores 0. The ranker will lean on the other scorers (added in
     * later commits) or the alphabetic tiebreak.
     */
    @Test
    fun `returns 0 when no rule in the tree matches the target conclusion`() {
        //Given a tree whose only rule has an unrelated conclusion
        val unrelatedRule = Rule(1, null, otherConclusion, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(unrelatedRule),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then
        score shouldBe 0
    }

    /**
     * The canonical happy path: one historical rule uses tshHigh for the
     * target conclusion, so a tshHigh candidate scores 1 and an unrelated
     * candidate scores 0.
     */
    @Test
    fun `scores 1 when one historical rule uses the condition for the target conclusion`() {
        //Given one historical rule using tshHigh for the target conclusion
        val historical = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then the matching condition scores 1, others score 0
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(tshLow)) shouldBe 0
        scorer.score(suggestionFor(mcvHigh)) shouldBe 0
    }

    /**
     * Multiple historical rules using the same condition should accumulate:
     * the signal is "how often was this condition used for this comment".
     */
    @Test
    fun `counts every historical rule that uses the condition`() {
        //Given three rules using tshHigh, and one rule for an unrelated
        //conclusion also using tshHigh (must not be counted)
        val r1 = Rule(1, null, goToBondi, setOf(tshHigh))
        val r2 = Rule(2, null, goToBondi, setOf(tshHigh, mcvHigh))
        val r3 = Rule(3, null, goToBondi, setOf(tshHigh))
        val unrelated = Rule(4, null, otherConclusion, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(r1, r2, r3, unrelated),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then tshHigh accumulates across the three matching rules; mcvHigh from r2 only
        scorer.score(suggestionFor(tshHigh)) shouldBe 3
        scorer.score(suggestionFor(mcvHigh)) shouldBe 1
    }

    /**
     * Matching is by `Conclusion.id`, not reference identity. KB reloads
     * create fresh `Conclusion` instances and this scorer must still find
     * the historical rules.
     */
    @Test
    fun `matches the target conclusion by id, not by reference identity`() {
        //Given a historical rule, and a freshly constructed Conclusion sharing
        //its id (modelling a KB reload that re-instantiates Conclusion objects)
        val historical = Rule(1, null, goToBondi, setOf(tshHigh))
        val reloadedGoToBondi = Conclusion(goToBondi.id, goToBondi.text)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddConclusion(reloadedGoToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the historical rule is found despite the new Conclusion instance
        score shouldBe 1
    }

    /**
     * ChangeTreeToReplaceConclusion's *replacement* — the comment being added
     * — is the target; the conclusion being replaced is ignored. This keeps
     * the scorer's behaviour aligned with Add: we surface conditions that the
     * KB has used to justify the comment the user is introducing.
     */
    @Test
    fun `replace action uses the replacement conclusion, not the one being replaced`() {
        //Given two rules — one using tshHigh for goToBondi (the replacement),
        //one using mcvHigh for otherConclusion (the comment being replaced)
        val usingTshHigh = Rule(1, null, goToBondi, setOf(tshHigh))
        val usingMcvHigh = Rule(2, null, otherConclusion, setOf(mcvHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToReplaceConclusion(
                toBeReplaced = otherConclusion,
                replacement = goToBondi,
            ),
            ruleTree = ruleTreeWith(usingTshHigh, usingMcvHigh),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then only the rule keyed off the replacement contributes
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(mcvHigh)) shouldBe 0
    }

    /**
     * Remove surfaces the conditions that previously gated the comment in, so
     * the user can see what they're competing against. This is informational,
     * not prescriptive (Phase 1 does not attempt to invert conditions).
     */
    @Test
    fun `remove action scores conditions that gated the removed conclusion in`() {
        //Given a historical rule that gated the to-be-removed conclusion in
        val historical = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToRemoveConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the historical condition is surfaced as a positive signal
        score shouldBe 1
    }
}
