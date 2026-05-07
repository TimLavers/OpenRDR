package io.rippledown.suggestions.scorer

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.signature.All
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

    // -----------------------------------------------------------------
    // Editable threshold (≥ / ≤) family-wise matching.
    //
    // Editable comparison candidates are auto-pinned to the current
    // case's reading. Exact predicate equality against historical rules
    // would be cutoff-sensitive — i.e. only match when the current case
    // happens to read the same value the historical rule was written
    // against. The scorer instead matches family-wise on
    //   (attribute, signature, comparison direction)
    // so that a candidate `eGFR ≥ 74.0` (auto-filled from the session
    // case) still recognises the historical rule `eGFR ≥ 70.0` as
    // relevant.
    // -----------------------------------------------------------------

    private val egfr = Attribute(12, "eGFR")
    private val ast = Attribute(13, "AST")

    private fun gteCandidate(
        attribute: Attribute,
        value: String,
        signature: io.rippledown.model.condition.episodic.signature.Signature = Current
    ) =
        EditableSuggestedCondition(
            EditableGreaterThanEqualsCondition(attribute, EditableValue(value, Type.Real), signature)
        )

    private fun lteCandidate(
        attribute: Attribute,
        value: String,
        signature: io.rippledown.model.condition.episodic.signature.Signature = Current
    ) =
        EditableSuggestedCondition(
            EditableLessThanEqualsCondition(attribute, EditableValue(value, Type.Real), signature)
        )

    /**
     * The motivating bug: a historical rule `eGFR ≥ 70` on the target
     * conclusion must promote the editable `eGFR ≥ <case value>`
     * candidate offered for a case whose eGFR happens to be a different
     * value (e.g. 74). Without family-wise matching the scorer would
     * return 0 here — the symptom that prompted this implementation.
     */
    @Test
    fun `editable greater-than-equals candidate matches historical greater-than-equals with a different cutoff`() {
        //Given a historical eGFR ≥ 70 rule and an editable candidate auto-filled to 74
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then the family-wise match fires despite the cutoff mismatch
        score shouldBe 1
    }

    /**
     * Symmetrically for ≤: editable `Hb ≤ 100` candidate matches a
     * historical `Hb ≤ 90` rule. Same family-wise rule applies.
     */
    @Test
    fun `editable less-than-equals candidate matches historical less-than-equals with a different cutoff`() {
        //Given a historical eGFR ≤ 90 rule and an editable candidate auto-filled to 100
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, LessThanOrEquals(90.0), Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(lteCandidate(egfr, "100.0"))

        //Then the family-wise match fires despite the cutoff mismatch
        score shouldBe 1
    }

    /**
     * Family-wise must NOT cross comparison directions: an editable
     * `≥` candidate is uninformed by a historical `≤` on the same
     * attribute (and vice versa). Mixing them would surface conditions
     * the KB has used in the *opposite* direction, which is misleading.
     */
    @Test
    fun `editable greater-than-equals candidate does not match historical less-than-equals on the same attribute`() {
        //Given a historical eGFR ≤ 70 rule and an editable ≥ candidate
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, LessThanOrEquals(70.0), Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then no match — opposite direction
        score shouldBe 0
    }

    /**
     * Family-wise must NOT cross attributes: a historical `eGFR ≥ 70`
     * rule does nothing for an editable `AST ≥ 36` candidate.
     */
    @Test
    fun `editable greater-than-equals candidate does not match historical greater-than-equals on a different attribute`() {
        //Given a historical eGFR ≥ 70 rule
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr, ast),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When the candidate is on a different attribute
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(ast, "36.0"))

        //Then no match
        score shouldBe 0
    }

    /**
     * Family-wise must NOT cross signatures: `Current ≥ 70` in the rule
     * tree should not boost an `All ≥ 70` editable candidate. Different
     * signatures express different intents (this episode vs every
     * episode) and are not interchangeable.
     */
    @Test
    fun `editable greater-than-equals candidate does not match historical condition with a different signature`() {
        //Given a historical (All) eGFR ≥ 70 rule but an editable Current candidate
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), All)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0", Current))

        //Then no match — signature mismatch
        score shouldBe 0
    }

    /**
     * Family-wise must NOT match a historical *symbolic* range predicate
     * (e.g. `is high`) for a numeric `≥` candidate. The scorer only
     * loosens the cutoff value, not the predicate family — promoting
     * `≥ <X>` whenever `is high` was used historically would conflate
     * two different rule shapes.
     */
    @Test
    fun `editable greater-than-equals candidate does not match historical is-high condition`() {
        //Given a historical "eGFR is high" rule
        val historical = Rule(
            1, null, goToBondi,
            setOf(EpisodicCondition(egfr, High, Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then no match — `High` is a different predicate family from `≥`
        score shouldBe 0
    }

    /**
     * Multiple historical rules using `≥` on the target conclusion all
     * accumulate, regardless of their individual cutoff values. The
     * count is the number of rules that historically reached for this
     * comparison family on this attribute.
     */
    @Test
    fun `editable greater-than-equals candidate accumulates across historical rules with varying cutoffs`() {
        //Given three historical eGFR ≥ X rules (different cutoffs) for the target
        //conclusion, plus an unrelated rule with the same predicate family on a
        //different conclusion (must not be counted)
        val r1 = Rule(1, null, goToBondi, setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)))
        val r2 = Rule(2, null, goToBondi, setOf(EpisodicCondition(egfr, GreaterThanOrEquals(75.0), Current)))
        val r3 = Rule(3, null, goToBondi, setOf(EpisodicCondition(egfr, GreaterThanOrEquals(80.0), Current)))
        val unrelated = Rule(
            4, null, otherConclusion,
            setOf(EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(r1, r2, r3, unrelated),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then all three matching rules count, the unrelated one does not
        score shouldBe 3
    }

    /**
     * A historical rule with multiple conditions still counts ONCE per
     * candidate, even if more than one of its conditions matches the
     * candidate's family. The score's unit is "historical rules", not
     * "matching conditions".
     */
    @Test
    fun `historical rule with multiple matching conditions counts once`() {
        //Given a historical rule whose conditions list happens to contain two
        //"eGFR ≥ X" entries (different cutoffs). This is unusual in practice
        //but a useful regression guard: candidate matching uses `Iterable.any`,
        //not `count`, so the rule must score 1 not 2 for the candidate.
        val historical = Rule(
            1, null, goToBondi,
            setOf(
                EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current),
                EpisodicCondition(egfr, GreaterThanOrEquals(80.0), Current),
            ),
        )
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val score = HistoricalRuleScorer(ctx).score(gteCandidate(egfr, "74.0"))

        //Then the rule counts once, not once per matching condition
        score shouldBe 1
    }

    /**
     * Regression guard for non-editable candidates: their matching is
     * still strict `sameAs`. `is high` ≠ `is low`, no family loosening.
     * This test exists to make sure the editable-comparison branch
     * doesn't accidentally relax matching for everybody.
     */
    @Test
    fun `non-editable candidate still uses strict sameAs matching`() {
        //Given a historical "tsh is high" rule
        val historical = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val scorer = HistoricalRuleScorer(ctx)

        //Then exact predicate match is required for non-editable candidates
        scorer.score(suggestionFor(tshHigh)) shouldBe 1
        scorer.score(suggestionFor(tshLow)) shouldBe 0
    }
}
