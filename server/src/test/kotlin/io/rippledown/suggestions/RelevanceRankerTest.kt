package io.rippledown.suggestions

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.NonEditableSuggestedCondition
import io.rippledown.model.condition.edit.SuggestedCondition
import io.rippledown.model.condition.episodic.predicate.High
import io.rippledown.model.condition.episodic.predicate.Low
import io.rippledown.model.condition.episodic.predicate.Normal
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.rule.*
import kotlin.test.Test

class RelevanceRankerTest {

    private val tsh = Attribute(10, "TSH")
    private val mcv = Attribute(11, "MCV")

    private val goToBondi = AssignValue(Attribute(100, "C1", AttributeKind.COMMENT), CommentTemplate("Go to Bondi."))

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

        //Then the ranked order is alphabetic by initial suggestion text
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
        //Given a history where tshHigh has been used for goToBondi, and a
        //candidate set in which mcvHigh would win on alphabetic order alone
        val history = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(history),
        )
        val candidates = listOf(suggestionFor(mcvHigh), suggestionFor(tshHigh))
        //Sanity: alphabetically mcvHigh sorts before tshHigh
        candidates.map { it.initialSuggestion().asText() }.sorted()
            .first() shouldBe mcvHigh.asText()

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then tshHigh wins on history, despite the alphabetic disadvantage
        ranked.first().initialSuggestion() shouldBe tshHigh
    }

    /**
     * Within a tier of equal historical scores, alphabetic order still
     * applies. Keeps results deterministic and preserves test intuition.
     */
    @Test
    fun `equal historical scores fall back to alphabetic order`() {
        //Given tshHigh and mcvHigh both scoring 1 historically
        val h1 = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val h2 = Rule(2, null, setOf(mcvHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(h1, h2),
        )
        val candidates = listOf(suggestionFor(tshHigh), suggestionFor(mcvHigh))

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then alphabetic order resolves the historical-score tie
        ranked.map { it.initialSuggestion() } shouldBe listOf(mcvHigh, tshHigh)
    }

    /**
     * A higher historical count outranks a lower one; a zero-history
     * candidate ends up last regardless of alphabetic advantage.
     */
    @Test
    fun `higher historical score ranks above lower historical score`() {
        //Given tshHigh used twice, mcvHigh once, tshLow never
        val r1 = Rule(1, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val r2 = Rule(2, null, setOf(tshHigh), mutableSetOf(), goToBondi)
        val r3 = Rule(3, null, setOf(mcvHigh), mutableSetOf(), goToBondi)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddAssignment(goToBondi),
            ruleTree = ruleTreeWith(r1, r2, r3),
        )
        val candidates = listOf(
            suggestionFor(tshLow),
            suggestionFor(tshHigh),
            suggestionFor(mcvHigh),
        )

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then candidates are ordered by descending historical score
        ranked.map { it.initialSuggestion() } shouldBe listOf(tshHigh, mcvHigh, tshLow)
    }

    /**
     * Comment-overlap from an assignment-based comment action must rank a
     * matching candidate above an alphabetically
     * earlier unrelated one, just as the conclusion-based action does.
     */
    @Test
    fun `comment overlap from assignment action beats alphabetic tiebreak`() {
        //Given an add-assignment action whose comment template is "TSH is high"
        val commentAttr = Attribute(100, "C1", AttributeKind.COMMENT)
        val ctx = SuggestionContext(
            sessionCase = sessionCase,
            attributes = setOf(tsh, mcv),
            action = ChangeTreeToAddAssignment(
                AssignValue(commentAttr, CommentTemplate("TSH is high"))
            ),
        )
        val candidates = listOf(suggestionFor(mcvHigh), suggestionFor(tshHigh))
        //Sanity: alphabetically mcvHigh sorts before tshHigh
        candidates.map { it.initialSuggestion().asText() }.sorted()
            .first() shouldBe mcvHigh.asText()

        //When
        val ranked = RelevanceRanker(ctx).rank(candidates)

        //Then tshHigh wins on comment overlap (tsh + high = 2), despite the
        //alphabetic disadvantage
        ranked.first().initialSuggestion() shouldBe tshHigh
    }
}
