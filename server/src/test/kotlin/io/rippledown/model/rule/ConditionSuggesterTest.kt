package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.HighOrNormalOrLow
import io.rippledown.model.condition.episodic.signature.*
import io.rippledown.suggestions.ConditionSuggester
import io.rippledown.suggestions.SuggestionContext
import io.rippledown.suggestions.editableReal
import kotlin.test.Test

/**
 * Test-only wrapper that exposes the uncapped generator output. The production
 * [ConditionSuggester.suggestions] caps the list at `MAX_SUGGESTIONS`, but every
 * assertion here is about the generator itself, independent of the cap.
 *
 * [substringAttributes] is the set of attributes that should be treated as
 * having been substring-matched in some prior rule, so that
 * [ConditionSuggester] generates `contains` / `does not contain` suggestions
 * for them. The wrapper builds a tiny rule tree with one child rule per such
 * attribute carrying a `Contains("")` predicate; the suggester only inspects
 * the rule tree to determine attribute eligibility for substring suggestions,
 * never to evaluate the rule.
 */
private class TestConditionSuggester(
    attributes: Set<Attribute>,
    sessionCase: RDRCase,
    substringAttributes: Set<Attribute> = emptySet(),
) {
    private val delegate = ConditionSuggester(
        SuggestionContext(
            sessionCase = sessionCase,
            attributes = attributes,
            ruleTree = ruleTreeWithSubstringRulesFor(substringAttributes),
        )
    )

    fun suggestions() = delegate.allSuggestions()

    companion object {
        private fun ruleTreeWithSubstringRulesFor(attrs: Set<Attribute>): RuleTree {
            val tree = RuleTree()
            attrs.forEachIndexed { idx, attr ->
                val rule = Rule(
                    id = idx + 1,
                    conditions = setOf(EpisodicCondition(attr, Contains(""), Current)),
                )
                tree.root.addChild(rule)
            }
            return tree
        }
    }
}

private fun conditionSuggester(
    attributes: Set<Attribute>,
    sessionCase: RDRCase,
    substringAttributes: Set<Attribute> = emptySet(),
) = TestConditionSuggester(attributes, sessionCase, substringAttributes)

internal class ConditionSuggesterTest {
    private val stuff = "stuff"
    private val things = "things"
    private val whatever = "whatever"
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

    private fun containsTextSuggestion(attribute: Attribute, text: String, signature: Signature = Current) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, signature))
    private fun doesNotContainTextSuggestion(attribute: Attribute, signature: Signature = Current) = EditableSuggestedCondition(EditableDoesNotContainCondition(attribute, signature))
    private fun isSingleEpisodeCaseSuggestion() = NonEditableSuggestedCondition(isSingleEpisodeCase())
    private fun isPresentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isPresent(attribute))
    private fun isAbsentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isAbsent(attribute))
    private fun rangeFilter(attribute: Attribute) = EpisodicCondition(attribute, HighOrNormalOrLow, AtLeast(1))
    private fun isLowSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isLow(null, attribute), rangeFilter(attribute))
    private fun isNormalSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isNormal(null, attribute), rangeFilter(attribute))
    private fun isHighSuggestion(attribute: Attribute) =
        NonEditableSuggestedCondition(isHigh(null, attribute, ""), rangeFilter(attribute))
    private fun isValueSuggestion(attribute: Attribute, value: String, signature: Signature = Current) = NonEditableSuggestedCondition(isCondition(null, attribute, value, signature))
    private fun isNumericSuggestion(attribute: Attribute, signature: Signature) = NonEditableSuggestedCondition(isNumeric( attribute,null, signature))
    private fun notNumericSuggestion(attribute: Attribute, signature: Signature) = NonEditableSuggestedCondition(notNumeric( attribute, null, signature))
    private fun greaterThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableGreaterThanEqualsCondition(attribute, EditableValue(value, Type.Real), Current))
    private fun lessThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableLessThanEqualsCondition(attribute, EditableValue(value, Type.Real), Current))
    private fun lowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowRangeCondition(attribute, Current))
    private fun normalOrLowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowNormalRangeCondition(attribute, Current))
    private fun normalOrHighByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighNormalRangeCondition(attribute, Current))
    private fun highByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighRangeCondition(attribute, Current))
    private fun valuesIncreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(increasing(attribute))
    private fun valuesDecreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(decreasing(attribute))

    @Test
    fun `single attribute single episode with textual value`() {
        val sessionCase = case(a to stuff)
        with(conditionSuggester(setOf(a), sessionCase, substringAttributes = setOf(a)).suggestions()) {
            this shouldContain isSingleEpisodeCaseSuggestion()
            this shouldContain isValueSuggestion(a, stuff)
            this shouldContain containsTextSuggestion(a, stuff)
            this shouldContain doesNotContainTextSuggestion(a)
            // IsPresentInCase (`attribute is in case`) was pruned: it is
            // tautological for attributes already in the session case.
            this shouldNotContain isPresentSuggestion(a)
            // IsNotNumeric/IsNumeric suggestions were pruned as low value.
            this shouldNotContain notNumericSuggestion(a, Current)
            this shouldNotContain isNumericSuggestion(a, Current)
        }
    }

    @Test
    fun `single attribute multiple episodes with two identical textual values`() {
        val sessionCase = multiEpisodeCase(a, things, things)
        with(conditionSuggester(setOf(a), sessionCase, substringAttributes = setOf(a)).suggestions()) {
            // Only Current / All / No signatures are generated now; the
            // AtLeast(n) / AtMost(n) signatures were pruned as noise.
            this shouldContain isValueSuggestion(a, things, Current)
            this shouldContain isValueSuggestion(a, things, All)
            this shouldNotContain isValueSuggestion(a, things, AtLeast(1))
            this shouldNotContain isValueSuggestion(a, things, AtLeast(2))
            this shouldNotContain isValueSuggestion(a, things, AtMost(2))

            // Contains/DoesNotContain are only generated for the Current
            // signature (and only for non-numeric values) after the prune.
            this shouldContain doesNotContainTextSuggestion(a, Current)
            this shouldContain containsTextSuggestion(a, things, Current)
            this shouldNotContain doesNotContainTextSuggestion(a, All)
            this shouldNotContain containsTextSuggestion(a, things, All)

            // IsPresent / IsNumeric / IsNotNumeric all pruned.
            this shouldNotContain isPresentSuggestion(a)
            this shouldNotContain notNumericSuggestion(a, Current)
            this shouldNotContain isNumericSuggestion(a, AtMost(1))
        }
    }

    @Test
    fun `single attribute multiple episodes with three identical textual values`() {
        val sessionCase = multiEpisodeCase(a, things, things, things)
        with(conditionSuggester(setOf(a), sessionCase, substringAttributes = setOf(a)).suggestions()) {
            this shouldContain isValueSuggestion(a, things, Current)
            this shouldContain isValueSuggestion(a, things, All)
            this shouldNotContain isValueSuggestion(a, things, AtLeast(1))
            this shouldNotContain isValueSuggestion(a, things, AtMost(3))

            this shouldContain doesNotContainTextSuggestion(a, Current)
            this shouldContain containsTextSuggestion(a, things, Current)
            this shouldNotContain doesNotContainTextSuggestion(a, All)
            this shouldNotContain containsTextSuggestion(a, things, All)

            this shouldNotContain isPresentSuggestion(a)
            this shouldNotContain notNumericSuggestion(a, Current)
            this shouldNotContain isNumericSuggestion(a, AtMost(1))
        }
    }

    @Test
    fun `single attribute multiple episodes with four identical textual values`() {
        val sessionCase = multiEpisodeCase(a, things, things, things, things)
        with(conditionSuggester(setOf(a), sessionCase, substringAttributes = setOf(a)).suggestions()) {
            this shouldContain isValueSuggestion(a, things, Current)
            this shouldContain isValueSuggestion(a, things, All)
            this shouldNotContain isValueSuggestion(a, things, AtLeast(1))

            this shouldContain doesNotContainTextSuggestion(a, Current)
            this shouldContain containsTextSuggestion(a, things, Current)
            this shouldNotContain doesNotContainTextSuggestion(a, All)
            this shouldNotContain containsTextSuggestion(a, things, All)

            this shouldNotContain isPresentSuggestion(a)
            this shouldNotContain notNumericSuggestion(a, Current)
            this shouldNotContain isNumericSuggestion(a, AtMost(1))
        }
    }

    @Test
    fun `single attribute multiple episodes with three textual values`() {
        val sessionCase = multiEpisodeCase(a, stuff, things, whatever)
        with(conditionSuggester(setOf(a), sessionCase, substringAttributes = setOf(a)).suggestions()) {
            // Contains / DoesNotContain are generated only at the Current
            // signature now.
            this shouldContain containsTextSuggestion(a, whatever, Current)
            this shouldContain doesNotContainTextSuggestion(a, Current)
            this shouldNotContain containsTextSuggestion(a, whatever, All)
            this shouldNotContain containsTextSuggestion(a, whatever, AtLeast(1))
            this shouldNotContain doesNotContainTextSuggestion(a, All)

            this shouldContain isValueSuggestion(a, whatever, Current)
            this shouldNotContain isValueSuggestion(a, whatever, AtLeast(1))
            this shouldNotContain isValueSuggestion(a, whatever, AtMost(1))

            this shouldNotContain isPresentSuggestion(a)
            this shouldNotContain notNumericSuggestion(a, Current)
            this shouldNotContain isNumericSuggestion(a, AtMost(1))
        }
    }

    @Test
    fun `single attribute multiple episodes with increasing numerical values`() {
        with(conditionSuggester(setOf(a), multiEpisodeCase(a, "1", "2", "3")).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "3")
            this shouldContain valuesIncreasingSuggestion(a)
            this shouldNotContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute multiple episodes with mixed numerical values`() {
        with(conditionSuggester(setOf(a), multiEpisodeCase(a, "1", "5", "3")).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "3")
            this shouldNotContain valuesIncreasingSuggestion(a)
            this shouldNotContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute multiple episodes with decreasing numerical values`() {
        with(conditionSuggester(setOf(a), multiEpisodeCase(a, "10", "5", "3")).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "3")
            this shouldNotContain valuesIncreasingSuggestion(a)
            this shouldContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with numerical value`() {
        val sessionCase = case(a to "1")
        with(conditionSuggester(setOf(a), sessionCase).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "1")
            this shouldNotContain valuesIncreasingSuggestion(a)
            this shouldNotContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with low value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("2.0", "10") ))
        with(conditionSuggester(setOf(a), sessionCase).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "1.0")
            this shouldContain isLowSuggestion(a)
            this shouldNotContain isNormalSuggestion(a)
            this shouldNotContain isHighSuggestion(a)
            // ExtendedRange ("by at most N%") suggestions were pruned.
            this shouldNotContain lowByAtMostSuggestion(a)
            this shouldNotContain normalOrLowByAtMostSuggestion(a)
            this shouldNotContain normalOrHighByAtMostSuggestion(a)
            this shouldNotContain highByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with normal value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("0", "10") ))
        with(conditionSuggester(setOf(a), sessionCase).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "1.0")
            this shouldNotContain isLowSuggestion(a)
            this shouldContain isNormalSuggestion(a)
            this shouldNotContain isHighSuggestion(a)
            this shouldNotContain lowByAtMostSuggestion(a)
            this shouldNotContain normalOrLowByAtMostSuggestion(a)
            this shouldNotContain normalOrHighByAtMostSuggestion(a)
            this shouldNotContain highByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with high value`() {
        val sessionCase = makeCase(a to tr("3.0", rr("0", "2.0") ))
        with(conditionSuggester(setOf(a), sessionCase).suggestions()) {
            checkContainsStandard3ForNumericValue(this, a, "3.0")
            this shouldNotContain isLowSuggestion(a)
            this shouldNotContain isNormalSuggestion(a)
            this shouldContain isHighSuggestion(a)
            this shouldNotContain lowByAtMostSuggestion(a)
            this shouldNotContain normalOrLowByAtMostSuggestion(a)
            this shouldNotContain normalOrHighByAtMostSuggestion(a)
            this shouldNotContain highByAtMostSuggestion(a)
        }
    }

    @Test
    fun `two attributes, one in of which is not in the case, one episode`() {
        val sessionCase = case(a to stuff)
        with(conditionSuggester(setOf(a, b), sessionCase, substringAttributes = setOf(a, b)).suggestions()) {
            checkContainsStandard2(this, a, stuff)
            this shouldContain isSingleEpisodeCaseSuggestion()
            this shouldContain doesNotContainTextSuggestion(a)
            // IsNotNumeric was pruned.
            this shouldNotContain notNumericSuggestion(a, Current)

            // IsAbsentFromCase (`attribute is not in case`) was pruned as
            // low value alongside IsPresentInCase.
            this shouldNotContain isAbsentSuggestion(b)
        }
    }

    @Test
    fun `two attributes, both of which are in the case, one episode`() {
        val sessionCase = case(a to stuff, b to things)
        with(conditionSuggester(setOf(a, b), sessionCase, substringAttributes = setOf(a, b)).suggestions()) {
            checkContainsStandard2(this, a, stuff)
            this shouldContain doesNotContainTextSuggestion(a)
            this shouldNotContain notNumericSuggestion(a, Current)

            checkContainsStandard2(this, b, things)
            this shouldContain doesNotContainTextSuggestion(b)
            this shouldNotContain notNumericSuggestion(b, Current)

            this shouldContain isSingleEpisodeCaseSuggestion()
        }
    }

    @Test
    fun `conditions are sorted`() {
        val sessionCase = case(a to "1", b to "2")
        val conditions = conditionSuggester(setOf(a, b), sessionCase).suggestions().map { it.asText() }
        conditions.sorted() shouldBe conditions
    }

    @Test
    fun editableValueTest() {
        editableReal(null) shouldBe null
        editableReal(tr("whatever")) shouldBe null
        editableReal(tr("123.99")) shouldBe EditableValue("123.99", Type.Real)
    }

    /**
     * Baseline suggestions expected for a TEXT-valued attribute in the
     * case after the prune: an `is <value>` equality suggestion plus a
     * free-text `contains <value>` (Current signature only). IsPresent
     * was dropped as tautological.
     */
    private fun checkContainsStandard2(conditions: List<SuggestedCondition>, attribute: Attribute, value: String) {
        conditions shouldContain isValueSuggestion(attribute, value)
        conditions shouldContain containsTextSuggestion(attribute, value)
        conditions shouldNotContain isPresentSuggestion(attribute)
    }

    /**
     * Baseline suggestions expected for a NUMERIC-valued attribute in the
     * case. Contains / DoesNotContain are not generated for numeric
     * values (they produce nonsense like `contains "194"`), IsNumeric and
     * IsPresent are pruned. The exact-equality `is "<value>"` variant is
     * also pruned for numeric values: it pins the threshold to this
     * case's reading and the `≥ <editable>` / `≤ <editable>` cutoffs
     * already cover the numeric-threshold intent with a user-editable
     * cutoff. Editable >=/<= cutoffs remain.
     */
    private fun checkContainsStandard3ForNumericValue(
        conditions: List<SuggestedCondition>,
        attribute: Attribute,
        value: String
    ) {
        conditions shouldNotContain isValueSuggestion(attribute, value)
        conditions shouldContain greaterThanOrEqualsSuggestion(attribute, value)
        conditions shouldContain lessThanOrEqualsSuggestion(attribute, value)
        conditions shouldNotContain containsTextSuggestion(attribute, value)
        conditions shouldNotContain isNumericSuggestion(attribute, Current)
        conditions shouldNotContain isPresentSuggestion(attribute)
    }
}