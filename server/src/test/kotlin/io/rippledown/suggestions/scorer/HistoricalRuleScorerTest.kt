package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.*
import io.rippledown.suggestions.SuggestionContext
import kotlin.test.Test

class HistoricalRuleScorerTest {

    private val tsh = Attribute(10, "TSH")
    private val mcv = Attribute(11, "MCV")

    private val commentAttr = Attribute(100, "C1", AttributeKind.COMMENT)
    private val otherCommentAttr = Attribute(101, "C2", AttributeKind.COMMENT)

    private val goToBondi = AssignValue(commentAttr, CommentTemplate("Go to Bondi."))
    private val otherAssignment = AssignValue(otherCommentAttr, CommentTemplate("Go to Manly."))

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
     * No action on the context means no target assignment — the scorer has
     * nothing to anchor on, so every candidate scores 0.
     */
    @Test
    fun `returns 0 for every candidate when action is null`() {
        //Given a tree with a rule using tshHigh for Go to Bondi but no active action
        val rule = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
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
     * Cold start: no rule in the tree has the target assignment, so every
     * candidate scores 0. The ranker will lean on the other scorers (added in
     * later commits) or the alphabetic tiebreak.
     */
    @Test
    fun `returns 0 when no rule in the tree matches the target assignment`() {
        //Given a tree whose only rule has an unrelated assignment
        val unrelatedRule = Rule(1, null, setOf(tshHigh), mutableSetOf(), otherAssignment)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(unrelatedRule),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then
        score shouldBe 0
    }

    /**
     * The canonical happy path: one historical rule uses tshHigh for the
     * target assignment, so a tshHigh candidate scores 1 and an unrelated
     * candidate scores 0.
     */
    @Test
    fun `scores 1 when one historical rule uses the condition for the target assignment`() {
        //Given one historical rule using tshHigh for the target assignment
        val historical = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(goToBondi),
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
        //assignment also using tshHigh (must not be counted)
        val r1 = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val r2 = Rule(2, null, setOf(tshHigh, mcvHigh), mutableSetOf(), goToBondi)
        val r3 = Rule(3, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val unrelated = Rule(4, null, setOf(tshHigh), mutableSetOf(), otherAssignment)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(r1, r2, r3, unrelated),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then tshHigh accumulates across the three matching rules; mcvHigh from r2 only
        scorer.score(suggestionFor(tshHigh)) shouldBe 3
        scorer.score(suggestionFor(mcvHigh)) shouldBe 1
    }

    /**
     * Matching is by attribute id, not reference identity. KB reloads
     * create fresh `Attribute` instances and this scorer must still find
     * the historical rules.
     */
    @Test
    fun `matches the target assignment by attribute id, not by reference identity`() {
        //Given a historical rule, and a freshly constructed AssignValue with an
        //Attribute sharing its id (modelling a KB reload that re-instantiates objects)
        val historical = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val reloadedAttr = Attribute(commentAttr.id, commentAttr.name, AttributeKind.COMMENT)
        val reloadedGoToBondi = AssignValue(reloadedAttr, CommentTemplate("Go to Bondi."))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(reloadedGoToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the historical rule is found despite the new Attribute instance
        score shouldBe 1
    }

    /**
     * ChangeTreeToReplaceAssignment's *replacement* — the comment being added
     * — is the target; the assignment being replaced is ignored. This keeps
     * the scorer's behaviour aligned with Add: we surface conditions that the
     * KB has used to justify the comment the user is introducing.
     */
    @Test
    fun `replace action uses the replacement assignment, not the one being replaced`() {
        //Given two rules — one using tshHigh for goToBondi (the replacement),
        //one using mcvHigh for otherAssignment (the comment being replaced)
        val usingTshHigh = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val usingMcvHigh = Rule(2, null, setOf(mcvHigh), mutableSetOf(), otherAssignment)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToReplaceAssignment(
                toBeReplaced = otherAssignment,
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
    fun `remove action scores conditions that gated the removed assignment in`() {
        //Given a historical rule that gated the to-be-removed assignment in
        val historical = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToRemoveAssignment(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the historical condition is surfaced as a positive signal
        score shouldBe 1
    }

    // -----------------------------------------------------------------
    // Editable threshold (≥ / ≤) matching is strict `sameAs`.
    //
    // The clinical-cutoff signal — "the KB has used `eGFR ≥ 70` for
    // this conclusion before" — is delivered by [ConditionSuggester]
    // injecting historical literals as candidates in their own right.
    // With those literals in the candidate pool, this scorer can match
    // them via plain `sameAs`. Loosening the matcher to family-wise
    // would also boost the case-pinned editable `eGFR ≥ 74` on the same
    // signal, which is exactly the behaviour the injection design
    // replaces.
    // -----------------------------------------------------------------

    private val egfr = Attribute(12, "eGFR")

    private fun gteCandidate(
        attribute: Attribute,
        value: String,
        signature: io.rippledown.model.condition.episodic.signature.Signature = Current
    ) =
        EditableSuggestedCondition(
            EditableGreaterThanEqualsCondition(attribute, EditableValue(value, Type.Real), signature)
        )

    /**
     * Editable candidate with the *same* cutoff as the historical rule
     * matches via `sameAs` — this is the path that surfaces clinical
     * cutoffs after [ConditionSuggester] has injected them as literal
     * candidates whose initial value matches the historical threshold.
     */
    @Test
    fun `editable greater-than-equals candidate matches historical greater-than-equals with the same cutoff`() {
        //Given a historical eGFR ≥ 70 rule and an editable candidate auto-filled to 70
        val historical = Rule(
            1, null, setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)),
            mutableSetOf(), goToBondi,
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "70.0"))

        //Then `sameAs` matches and the score is 1
        score shouldBe 1
    }

    /**
     * Editable candidate with a *different* cutoff does NOT match the
     * historical rule. The clinical-cutoff signal must come through the
     * literal historical condition that `ConditionSuggester` injects
     * into the candidate set — not through this scorer relaxing its
     * matcher to family-wise across cutoff values.
     */
    @Test
    fun `editable greater-than-equals candidate does not match historical greater-than-equals with a different cutoff`() {
        //Given a historical eGFR ≥ 70 rule and an editable candidate auto-filled to 74
        val historical = Rule(
            1, null, setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)),
            mutableSetOf(), goToBondi,
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then no match — strict `sameAs` requires the same cutoff value
        score shouldBe 0
    }

    /**
     * A historical rule with multiple conditions still counts ONCE per
     * candidate, even if more than one of its conditions `sameAs` the
     * candidate. The score's unit is "historical rules", not "matching
     * conditions".
     */
    @Test
    fun `historical rule with multiple matching conditions counts once`() {
        //Given a historical rule whose conditions list contains two `tshHigh`
        //entries (unusual but a useful regression guard: candidate matching
        //uses `Iterable.any`, not `count`).
        val historical = Rule(
            1, null,
            setOf(
                EpisodicCondition(null, tsh, High, Current, "tsh is elevated"),
                EpisodicCondition(null, tsh, High, Current, "tsh is up"),
            ),
            mutableSetOf(), goToBondi,
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the rule counts once, not once per matching condition
        score shouldBe 1
    }

    /**
     * Regression guard for non-editable candidates: matching is strict
     * `sameAs`. `is high` ≠ `is low`.
     */
    @Test
    fun `non-editable candidate uses strict sameAs matching`() {
        //Given a historical "tsh is high" rule
        val historical = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then exact predicate match is required for non-editable candidates
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(tshLow)) shouldBe 0
    }

    // -----------------------------------------------------------------
    // Assignment-based actions: comments are comment attributes
    // -----------------------------------------------------------------

    /**
     * A historical rule that assigns the same comment attribute as the
     * action's target must be found by the scorer, so its conditions
     * contribute to the historical signal.
     */
    @Test
    fun `assignment add action matches historical rules by target attribute id`() {
        //Given a historical rule assigning commentAttr with tshHigh
        val historical = Rule(
            1, null, setOf(tshHigh), mutableSetOf(),
            assignment = AssignValue(commentAttr, ByDefinition)
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(AssignValue(commentAttr, ByDefinition)),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then the matching condition scores 1, unrelated conditions score 0
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(tshLow)) shouldBe 0
    }

    /**
     * A rule assigning a *different* comment attribute must not be matched,
     * so its conditions don't leak into the signal for the current comment.
     */
    @Test
    fun `assignment add action does not match rules with a different comment attribute`() {
        //Given a rule for a different comment attribute
        val unrelated = Rule(
            1, null, setOf(tshHigh), mutableSetOf(),
            assignment = AssignValue(otherCommentAttr, ByDefinition)
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddAssignment(AssignValue(commentAttr, ByDefinition)),
            ruleTree = ruleTreeWith(unrelated),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then no historical signal
        score shouldBe 0
    }

    /**
     * Replace uses the *replacement* attribute as the target, matching the
     * convention from the conclusion-based scorer.
     */
    @Test
    fun `assignment replace action uses the replacement attribute as target`() {
        //Given one rule for the replacement attribute and one for the original
        val usingTshHigh = Rule(
            1, null, setOf(tshHigh), mutableSetOf(),
            assignment = AssignValue(commentAttr, ByDefinition)
        )
        val usingMcvHigh = Rule(
            2, null, setOf(mcvHigh), mutableSetOf(),
            assignment = AssignValue(otherCommentAttr, ByDefinition)
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToReplaceAssignment(
                AssignValue(otherCommentAttr, ByDefinition),
                AssignValue(commentAttr, ByDefinition)
            ),
            ruleTree = ruleTreeWith(usingTshHigh, usingMcvHigh),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then only the rule keyed off the replacement attribute contributes
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(mcvHigh)) shouldBe 0
    }

    /**
     * Remove uses the attribute being removed as the target.
     */
    @Test
    fun `assignment remove action scores conditions that gated the removed comment`() {
        //Given a historical rule that assigned the to-be-removed comment attribute
        val historical = Rule(
            1, null, setOf(tshHigh), mutableSetOf(),
            assignment = AssignValue(commentAttr, ByDefinition)
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToRemoveAssignment(AssignValue(commentAttr, ByDefinition)),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(suggestionFor(tshHigh))

        //Then the historical condition is surfaced
        score shouldBe 1
    }
}
