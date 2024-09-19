package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.*
import io.rippledown.model.condition.episodic.signature.*
import kotlin.test.Test

internal class ConditionSuggesterTest {
    private val stuff = "stuff"
    private val things = "things"
    private val whatever = "whatever"
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

    private fun containsTextSuggestion(attribute: Attribute, text: String, signature: Signature = Current) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, signature))
    private fun doesNotContainTextSuggestion(attribute: Attribute, signature: Signature = Current) = EditableSuggestedCondition(EditableDoesNotContainCondition(attribute, signature))
    private fun atLeastContainsTextSuggestion(attribute: Attribute, text: String, count: Int) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, AtLeast(count)))
    private fun atMostContainsTextSuggestion(attribute: Attribute, text: String, count: Int) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, AtMost(count)))
    private fun allContainsTextSuggestion(attribute: Attribute, text: String, count: Int) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, All))
    private fun noneContainsTextSuggestion(attribute: Attribute, text: String, count: Int) = EditableSuggestedCondition(EditableContainsCondition(attribute, text, No))
    private fun isSingleEpisodeCaseSuggestion() = NonEditableSuggestedCondition(isSingleEpisodeCase())
    private fun isPresentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isPresent(attribute))
    private fun isAbsentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isAbsent(attribute))
    private fun isLowSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isLow(null, attribute))
    private fun isNormalSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isNormal(null, attribute))
    private fun isHighSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isHigh(null, attribute))
    private fun isValueSuggestion(attribute: Attribute, value: String, signature: Signature = Current) = NonEditableSuggestedCondition(isCondition(null, attribute, value, signature))
    private fun isNumericSuggestion(attribute: Attribute, signature: Signature) = NonEditableSuggestedCondition(isNumeric( attribute,null, signature))
    private fun notNumericSuggestion(attribute: Attribute, signature: Signature) = NonEditableSuggestedCondition(notNumeric( attribute, null, signature))
    private fun greaterThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableGreaterThanEqualsCondition(attribute, EditableValue(value, Type.Real), Current))
    private fun lessThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableLessThanEqualsCondition(attribute, EditableValue(value, Type.Real), Current))
    private fun lowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowRangeCondition(attribute, Current))
    private fun normalOrLowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowNormalRangeCondition(attribute, Current))
    private fun normalOrHighByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighNormalRangeCondition(attribute, Current))
    private fun highByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighRangeCondition(attribute, Current))
    private fun allValuesContainTextSuggestion(attribute: Attribute, text: String) = EditableSuggestedCondition(EditableContainsCondition(attribute, text))
    private fun valuesIncreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(increasing(attribute))
    private fun valuesDecreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(decreasing(attribute))
    private fun allNumericSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(allNumeric(attribute))
    private fun noneNumericSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(noneNumeric(attribute))

    @Test
    fun `single attribute single episode with textual value`() {
        val sessionCase = case(a to stuff)
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldHaveSize 5
            this shouldContain isSingleEpisodeCaseSuggestion()
            this shouldContain isPresentSuggestion(a)
            this shouldContain isValueSuggestion( a, stuff)
            this shouldContain containsTextSuggestion(a, stuff)
            this shouldContain doesNotContainTextSuggestion(a)
        }
    }

    @Test
    fun `single attribute multiple episodes with two identical textual values`() {
        val sessionCase = multiEpisodeCase(a, things, things)
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 27
            this shouldContain isPresentSuggestion(a)

            this shouldContain isValueSuggestion( a, things, Current)
            this shouldContain isValueSuggestion( a, things, All)
            this shouldContain isValueSuggestion( a, things, AtLeast(1))
            this shouldContain isValueSuggestion( a, things, AtLeast(2))
            this shouldContain isValueSuggestion( a, things, AtMost(2))

            this shouldContain doesNotContainTextSuggestion(a, Current)
            this shouldContain doesNotContainTextSuggestion(a, All)
            this shouldContain doesNotContainTextSuggestion(a, AtLeast(1))
            this shouldContain doesNotContainTextSuggestion(a, AtLeast(2))
            this shouldContain doesNotContainTextSuggestion(a, AtMost(1))
            this shouldContain doesNotContainTextSuggestion(a, AtMost(2))
            this shouldContain doesNotContainTextSuggestion(a, No)

            this shouldContain containsTextSuggestion(a, things, Current)
            this shouldContain containsTextSuggestion(a, things, All)
            this shouldContain containsTextSuggestion(a, things, AtLeast(1))
            this shouldContain containsTextSuggestion(a, things, AtLeast(2))
            this shouldContain containsTextSuggestion(a, things, AtMost(2))

            this shouldContain notNumericSuggestion(a, Current)
            this shouldContain notNumericSuggestion(a, All)
            this shouldContain notNumericSuggestion(a, AtLeast(1))
            this shouldContain notNumericSuggestion(a, AtLeast(2))
            this shouldContain notNumericSuggestion(a, AtMost(2))

            this shouldContain isNumericSuggestion(a, AtMost(1))
            this shouldContain isNumericSuggestion(a, AtMost(2))
            this shouldContain isNumericSuggestion(a, No)

            this shouldContain isSingleEpisodeCaseSuggestion()
        }
    }

    @Test
    fun `single attribute multiple episodes with three textual values`() {
        val sessionCase = multiEpisodeCase(a, "first", "second", "third")
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldContain isPresentSuggestion(a)
            this shouldContain isValueSuggestion( a, "third")
            this shouldContain containsTextSuggestion(a, "third")
        }
    }

    @Test
    fun `single attribute multiple episodes with many textual values`() {
        val sessionCase = multiEpisodeCase(a, "first", "second", "third")
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldContain isPresentSuggestion(a)
            this shouldContain isValueSuggestion( a, "third")
            this shouldContain containsTextSuggestion(a, "third")
        }
    }

    @Test
    fun `single attribute multiple episodes with increasing numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "1", "2", "3")).suggestions()) {
            this.size shouldBe 7
            checkContainsStandard6ForNumericValue(this, a, "3")
            this shouldContain valuesIncreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute multiple episodes with mixed numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "1", "5", "3")).suggestions()) {
            this.size shouldBe 6
            checkContainsStandard6ForNumericValue(this, a, "3")

        }
    }

    @Test
    fun `single attribute multiple episodes with decreasing numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "10", "5", "3")).suggestions()) {
            this.size shouldBe 7
            checkContainsStandard6ForNumericValue(this,a, "3")
            this shouldContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with numerical value`() {
        val sessionCase = case(a to "1")
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 6
            checkContainsStandard6ForNumericValue(this,a, "1")
        }
    }

    @Test
    fun `single attribute single episode with low value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("2.0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 9
            checkContainsStandard6ForNumericValue(this,a, "1.0")
            this shouldContain isLowSuggestion(a)
            this shouldContain lowByAtMostSuggestion(a)
            this shouldContain normalOrLowByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with normal value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 9
            checkContainsStandard6ForNumericValue(this,a, "1.0")
            this shouldContain isNormalSuggestion(a)
            this shouldContain normalOrLowByAtMostSuggestion(a)
            this shouldContain normalOrHighByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with high value`() {
        val sessionCase = makeCase(a to tr("3.0", rr("0", "2.0") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 9
            checkContainsStandard6ForNumericValue(this,a, "3.0")
            this shouldContain isHighSuggestion(a)
            this shouldContain normalOrHighByAtMostSuggestion(a)
            this shouldContain highByAtMostSuggestion(a)
        }
    }

    @Test
    fun `two attributes, one in of which is not in the case, one episode`() {
        val sessionCase = case(a to stuff)
        with(ConditionSuggester(setOf(a, b), sessionCase).suggestions()) {
            this.size shouldBe 4
            checkContainsStandard3(this, a, stuff)
            this shouldContain isAbsentSuggestion(b)
        }
    }

    @Test
    fun `two attributes, both of which are in the case, one episode`() {
        val sessionCase = case(a to stuff, b to things)
        with(ConditionSuggester(setOf(a, b), sessionCase).suggestions()) {
            this.size shouldBe 6
            checkContainsStandard3(this, a, stuff)
            checkContainsStandard3(this, b, things)
        }
    }

    @Test
    fun `conditions are sorted`() {
        val sessionCase = case(a to "1", b to "2")
        val conditions = ConditionSuggester(setOf(a, b), sessionCase).suggestions().map { it.asText() }
        conditions.sorted() shouldBe conditions
    }

    @Test
    fun editableValueTest() {
        editableReal(null) shouldBe null
        editableReal(tr("whatever")) shouldBe null
        editableReal(tr("123.99")) shouldBe EditableValue("123.99", Type.Real)
    }

    private fun checkContainsStandard3(conditions: List<SuggestedCondition>, attribute: Attribute, value: String) {
        conditions shouldContain isPresentSuggestion(attribute)
        conditions shouldContain isValueSuggestion(attribute, value)
        conditions shouldContain containsTextSuggestion(attribute, value)
    }

    private fun checkContainsStandard6ForNumericValue(conditions: List<SuggestedCondition>, attribute: Attribute, value: String) {
        checkContainsStandard3(conditions, attribute, value)
        conditions shouldContain greaterThanOrEqualsSuggestion( attribute, value)
        conditions shouldContain lessThanOrEqualsSuggestion( attribute, value)
        conditions shouldContain isNumericSuggestion( attribute, Current)
    }
}