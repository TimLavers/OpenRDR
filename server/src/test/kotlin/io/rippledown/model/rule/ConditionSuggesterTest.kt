package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.*
import kotlin.test.Test

internal class ConditionSuggesterTest {
    private val stuff = "stuff"
    private val things = "things"
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

    private fun containsTextSuggestion(attribute: Attribute, text: String) = EditableSuggestedCondition(EditableContainsCondition(attribute, text))
    private fun isPresentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isPresent(attribute))
    private fun isAbsentSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isAbsent(attribute))
    private fun isLowSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isLow(null, attribute))
    private fun isNormalSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isNormal(null, attribute))
    private fun isHighSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(isHigh(null, attribute))
    private fun isValueSuggestion(attribute: Attribute, value: String) = NonEditableSuggestedCondition(isCondition(null, attribute, value))
    private fun greaterThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableGTECondition(attribute, EditableValue(value, Type.Real)))
    private fun lessThanOrEqualsSuggestion(attribute: Attribute, value: String) = EditableSuggestedCondition(EditableLTECondition(attribute, EditableValue(value, Type.Real)))
    private fun lowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowRangeCondition(attribute))
    private fun normalOrLowByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedLowNormalRangeCondition(attribute))
    private fun normalOrHighByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighNormalRangeCondition(attribute))
    private fun highByAtMostSuggestion(attribute: Attribute) = EditableSuggestedCondition(EditableExtendedHighRangeCondition(attribute))
    private fun allValuesContainTextSuggestion(attribute: Attribute, text: String) = EditableSuggestedCondition(EditableContainsCondition(attribute, text))
    private fun valuesIncreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(increasing(attribute))
    private fun valuesDecreasingSuggestion(attribute: Attribute) = NonEditableSuggestedCondition(decreasing(attribute))

    @Test
    fun `single attribute single episode with textual value`() {
        val sessionCase = case(a to stuff)
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldHaveSize 3
            this shouldContain isPresentSuggestion(a)
            this shouldContain isValueSuggestion( a, stuff)
            this shouldContain containsTextSuggestion(a, stuff)
        }
    }

//    @Test
    fun `single attribute multiple episodes with textual value`() {
        TODO()
//        val sessionCase = multiEpisodeCase(a, "first", "second", "third")
//        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
//            this shouldContain isPresentSuggestion(a)
//            this shouldContain isValueSuggestion( a, "third")
//            this shouldContain containsTextSuggestion(a, "third")
//        }
    }

    @Test
    fun `single attribute multiple episodes with increasing numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "1", "2", "3")).suggestions()) {
            this.size shouldBe 6
            checkContainsStandard5ForNumericValue(this,a, "3")
            this shouldContain valuesIncreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute multiple episodes with mixed numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "1", "5", "3")).suggestions()) {
            this.size shouldBe 5
            checkContainsStandard5ForNumericValue(this,a, "3")
        }
    }

    @Test
    fun `single attribute multiple episodes with decreasing numerical values`() {
        with(ConditionSuggester(setOf(a), multiEpisodeCase(a, "10", "5", "3")).suggestions()) {
            this.size shouldBe 6
            checkContainsStandard5ForNumericValue(this,a, "3")
            this shouldContain valuesDecreasingSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with numerical value`() {
        val sessionCase = case(a to "1")
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 5
            checkContainsStandard5ForNumericValue(this,a, "1")
        }
    }

    @Test
    fun `single attribute single episode with low value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("2.0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 8
            checkContainsStandard5ForNumericValue(this,a, "1.0")
            this shouldContain isLowSuggestion(a)
            this shouldContain lowByAtMostSuggestion(a)
            this shouldContain normalOrLowByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with normal value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 8
            checkContainsStandard5ForNumericValue(this,a, "1.0")
            this shouldContain isNormalSuggestion(a)
            this shouldContain normalOrLowByAtMostSuggestion(a)
            this shouldContain normalOrHighByAtMostSuggestion(a)
        }
    }

    @Test
    fun `single attribute single episode with high value`() {
        val sessionCase = makeCase(a to tr("3.0", rr("0", "2.0") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 8
            checkContainsStandard5ForNumericValue(this,a, "3.0")
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
        conditions shouldContain isValueSuggestion( attribute, value)
        conditions shouldContain containsTextSuggestion( attribute, value)
    }

    private fun checkContainsStandard5ForNumericValue(conditions: List<SuggestedCondition>, attribute: Attribute, value: String) {
        checkContainsStandard3(conditions, attribute, value)
        conditions shouldContain greaterThanOrEqualsSuggestion( attribute, value)
        conditions shouldContain lessThanOrEqualsSuggestion( attribute, value)
    }
}