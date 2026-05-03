package io.rippledown.suggestions

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.ChangeTreeToAddConclusion
import io.rippledown.model.rule.Rule
import io.rippledown.model.rule.RuleTree
import io.rippledown.model.rule.case
import kotlin.test.Test

class RelevanceRankerTest {

    private val tsh = Attribute(10, "TSH")
    private val mcv = Attribute(11, "MCV")

    private val goToBondi = Conclusion(100, "Go to Bondi.")

    private val tshHigh = EpisodicCondition(tsh, High, Current)
    private val tshLow = EpisodicCondition(tsh, Low, Current)
    private val tshNormal = EpisodicCondition(tsh, Normal, Current)
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
     * With no action on the context, every scorer returns 0 and the ranker
     * must fall back to alphabetic order — matching the behaviour of the
     * old `Sorter` exactly. This is the regression guard for Phase 1's
     * "no behaviour change when there's no session" contract.
     */
    @Test
    fun `all-zero scores fall back to alphabetic order on initial suggestion text`() {
        //Given an empty context (no action, no rules, no cornerstones)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
        )
        val candidates = listOf(
            suggestionFor(tshNormal),
            suggestionFor(mcvHigh),
            suggestionFor(tshHigh),
            suggestionFor(tshLow),
        )

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then: alphabetic by asText()
        ranked.map { it.initialSuggestion().asText() } shouldBe
                candidates.map { it.initialSuggestion().asText() }.sorted()
    }

    /**
     * A condition with historical support for the action's target conclusion
     * must outrank an alphabetically-earlier condition with none. This is the
     * core Phase 1 guarantee: history beats alphabetic.
     */
    @Test
    fun `historical score beats alphabetic tiebreak`() {
        //Given a history where tshHigh has been used for goToBondi
        val history = Rule(1, null, goToBondi, setOf(tshHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(history),
        )

        //Sanity check: alphabetically, mcvHigh comes before tshHigh
        val candidates = listOf(suggestionFor(mcvHigh), suggestionFor(tshHigh))
        candidates.map { it.initialSuggestion().asText() }.sorted()
            .first() shouldBe mcvHigh.asText()

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then: tshHigh wins on history, despite alphabetic loss
        ranked.first().initialSuggestion() shouldBe tshHigh
    }

    /**
     * Within a tier of equal historical scores, alphabetic order still
     * applies. Keeps results deterministic and preserves test intuition.
     */
    @Test
    fun `equal historical scores fall back to alphabetic order`() {
        //Given tshHigh and mcvHigh both scoring 1 historically
        val h1 = Rule(1, null, goToBondi, setOf(tshHigh))
        val h2 = Rule(2, null, goToBondi, setOf(mcvHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(h1, h2),
        )
        val candidates = listOf(suggestionFor(tshHigh), suggestionFor(mcvHigh))

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then
        ranked.map { it.initialSuggestion() } shouldBe listOf(mcvHigh, tshHigh)
    }

    /**
     * A higher historical count outranks a lower one; a zero-history
     * candidate ends up last regardless of alphabetic advantage.
     */
    @Test
    fun `higher historical score ranks above lower historical score`() {
        //Given tshHigh used twice, mcvHigh once, tshLow never
        val r1 = Rule(1, null, goToBondi, setOf(tshHigh))
        val r2 = Rule(2, null, goToBondi, setOf(tshHigh))
        val r3 = Rule(3, null, goToBondi, setOf(mcvHigh))
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddConclusion(goToBondi),
            ruleTree = ruleTreeWith(r1, r2, r3),
        )
        val candidates = listOf(
            suggestionFor(tshLow),
            suggestionFor(tshHigh),
            suggestionFor(mcvHigh),
        )

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then
        ranked.map { it.initialSuggestion() } shouldBe listOf(tshHigh, mcvHigh, tshLow)
    }
}
