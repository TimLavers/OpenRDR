package io.rippledown.suggestions

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.EditableSuggestedCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.rr
import io.rippledown.model.condition.tr
import io.rippledown.model.rule.*
import kotlin.test.Test

/**
 * Tests for `ConditionSuggester`'s historical-condition injection. See
 * "Historical-condition injection" in
 * `documentation/design/targeted_suggested_conditions.md`.
 *
 * The motivating use case: a historical rule `eGFR ≥ 70` for the same
 * conclusion must surface the literal `eGFR ≥ 70` as a suggestion when
 * the user adds that conclusion to a case whose eGFR happens to be 74,
 * because clinical cutoffs from existing rules are usually the
 * evidence-based ones the user wants to reach for. The case-pinned
 * editable `eGFR ≥ 74` continues to appear alongside (with the cutoff
 * editable) but is no longer the only option.
 */
class HistoricalConditionInjectionTest {

    private val egfr = Attribute(12, "eGFR")
    private val tsh = Attribute(10, "TSH")

    private val goToBondi = Conclusion(100, "Go to Bondi.")
    private val otherConclusion = Conclusion(101, "Go to Manly.")

    private fun ruleTreeWith(vararg rules: Rule): RuleTree {
        val root = Rule(0)
        rules.forEach { root.addChild(it) }
        return RuleTree(root)
    }

    private fun List<io.rippledown.model.condition.edit.SuggestedCondition>.containsConditionSameAs(
        target: io.rippledown.model.condition.Condition,
    ): Boolean = any { it.initialSuggestion().sameAs(target) }

    @Test
    fun `historical literal greater-than-equals condition is injected as a candidate when it holds on the session case`() {
        //Given a case whose eGFR reads 74 and a historical eGFR ≥ 70 rule
        val sessionCase = case(egfr to "74")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then both the historical literal and the case-pinned editable appear.
        //(Editable conditions don't have value-based equals, so we compare via
        //`sameAs` on the initial suggestion rather than `shouldContain`.)
        suggestions shouldContain NonEditableSuggestedCondition(egfrGte70)
        suggestions.containsConditionSameAs(
            EpisodicCondition(egfr, GreaterThanOrEquals(74.0), Current)
        ) shouldBe true
    }

    @Test
    fun `historical literal condition that does NOT hold on the session case is not injected`() {
        //Given a case whose eGFR reads 60 and a historical eGFR ≥ 70 rule —
        //the historical condition would never fire on this case, so we must
        //not surface it.
        val sessionCase = case(egfr to "60")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then the historical literal is NOT offered
        suggestions shouldNotContain NonEditableSuggestedCondition(egfrGte70)
    }

    @Test
    fun `historical literal is ranked above the case-pinned editable on the same attribute`() {
        //Given the motivating Phase 1 scenario: case eGFR = 74 with a historical
        //eGFR ≥ 70 rule for the action's target conclusion.
        val sessionCase = case(egfr to "74")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then the historical literal precedes the case-pinned editable
        val literalIndex = suggestions.indexOfFirst { it.initialSuggestion().sameAs(egfrGte70) }
        val editableIndex = suggestions.indexOfFirst {
            val cond = it.initialSuggestion()
            cond is EpisodicCondition &&
                    cond.attribute.isEquivalent(egfr) &&
                    cond.predicate == GreaterThanOrEquals(74.0) &&
                    cond.signature == Current
        }
        literalIndex shouldBe 0
        (literalIndex < editableIndex) shouldBe true
    }

    @Test
    fun `historical literal is not duplicated when the case value happens to match the historical cutoff`() {
        //Given a case whose eGFR is exactly 70 — the case-pinned editable is
        //sameAs the historical literal, so we must not offer two structurally
        //identical entries. The editable form wins (user can still adjust).
        val sessionCase = case(egfr to "70")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then exactly one ≥-on-eGFR suggestion is offered, and it is the editable form
        val gteOnEgfr = suggestions.filter {
            val cond = it.initialSuggestion()
            cond is EpisodicCondition &&
                    cond.attribute.isEquivalent(egfr) &&
                    cond.predicate is GreaterThanOrEquals
        }
        gteOnEgfr.size shouldBe 1
        (gteOnEgfr.single() is EditableSuggestedCondition) shouldBe true
    }

    @Test
    fun `historical conditions for unrelated conclusions are not injected`() {
        //Given a historical rule that uses eGFR ≥ 70 for a DIFFERENT conclusion
        //than the one the user is currently adding.
        val sessionCase = case(egfr to "74")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, otherConclusion, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then the unrelated historical literal is NOT injected
        suggestions shouldNotContain NonEditableSuggestedCondition(egfrGte70)
    }

    @Test
    fun `historical literal is injected for symbolic predicates too, deduped against the existing candidate set`() {
        //Given a historical "tsh is high" rule and a case where TSH is high
        //(value above the reference range). The generator will already produce
        //an `is high` candidate for this case; the historical injection must
        //dedup against it rather than offering it twice.
        val sessionCase = makeCase(tsh to tr("12.0", rr("0", "10")))
        val tshHigh = EpisodicCondition(tsh, High, Current)
        val historical = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then exactly one `tsh is high` candidate is present
        val tshHighCount = suggestions.count { it.initialSuggestion().sameAs(tshHigh) }
        tshHighCount shouldBe 1
    }

    @Test
    fun `historical conditions are not injected when the action is null`() {
        //Given no active action — the no-session path that must keep
        //today's behaviour (alphabetic fallback, no historical interference).
        val sessionCase = case(egfr to "74")
        val egfrGte70 = EpisodicCondition(egfr, GreaterThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrGte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = null,
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then the historical literal is NOT injected
        suggestions shouldNotContain NonEditableSuggestedCondition(egfrGte70)
    }

    @Test
    fun `historical less-than-equals literal is injected when it holds on the session case`() {
        //Symmetric guard for ≤ — the bug originally reported was specific to
        //≥ but the injection must work for both directions.
        val sessionCase = case(egfr to "60")
        val egfrLte70 = EpisodicCondition(egfr, LessThanOrEquals(70.0), Current)
        val historical = Rule(1, null, goToBondi, setOf(egfrLte70))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(egfr),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(historical),
        )

        //When
        val suggestions = ConditionSuggester(ctx).suggestions()

        //Then the literal `eGFR ≤ 70` is injected as a candidate
        suggestions shouldContain NonEditableSuggestedCondition(egfrLte70)
    }
}
