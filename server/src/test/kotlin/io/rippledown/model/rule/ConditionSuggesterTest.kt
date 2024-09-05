package io.rippledown.model.rule

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.edit.EditableValue
import io.rippledown.model.condition.edit.Type
import kotlin.test.Test

internal class ConditionSuggesterTest {
    private val stuff = "stuff"
    private val things = "things"
    val a = Attribute(0, "A")
    val b = Attribute(1, "B")

//    @Test
    fun `single attribute with textual value`() {
        val sessionCase = case(a to stuff)
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldHaveSize 3
            this shouldContain isPresent(a)
            this shouldContain isCondition(null, a, stuff)
            this shouldContain containsText(null, a, stuff)
        }
    }

//    @Test
    fun `single attribute with numerical value`() {
        val sessionCase = case(a to "1")
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 5
            this shouldContain isPresent(a)
            this shouldContain isCondition(null, a, "1")
            this shouldContain containsText(null, a, "1")
            this shouldContain greaterThanOrEqualTo(null, a, 1.0)
            this shouldContain lessThanOrEqualTo(null, a, 1.0)
        }
    }

//    @Test
    fun `single attribute with numerical value and low value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("2.0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this.size shouldBe 6
            this shouldContain isPresent(a)
            this shouldContain isCondition(null, a, "1.0")
            this shouldContain containsText(null, a, "1.0")
            this shouldContain greaterThanOrEqualTo(null, a, 1.0)
            this shouldContain lessThanOrEqualTo(null, a, 1.0)
            this shouldContain isLow(null, a)
        }
    }

//    @Test
    fun `single attribute with numerical value and normal value`() {
        val sessionCase = makeCase(a to tr("1.0", rr("0", "10") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldContain isNormal(null, a)
        }
    }

//    @Test
    fun `single attribute with numerical value and high value`() {
        val sessionCase = makeCase(a to tr("3.0", rr("0", "2.0") ))
        with(ConditionSuggester(setOf(a), sessionCase).suggestions()) {
            this shouldContain isHigh(null, a)
        }
    }

//    @Test
    fun `two attributes, one in of which is in the case`() {
        val sessionCase = case(a to stuff)
        with(ConditionSuggester(setOf(a, b), sessionCase).suggestions()) {
            this.size shouldBe 4
            this shouldContain isPresent(a)
            this shouldContain isCondition(null, a, stuff)
            this shouldContain containsText(null, a, stuff)
            this shouldContain isAbsent(b)
        }
    }

//    @Test
    fun `two attributes, both of which are in the case`() {
        val sessionCase = case(a to stuff, b to things)
        with(ConditionSuggester(setOf(a, b), sessionCase).suggestions()) {
            this.size shouldBe 6
            this shouldContain isPresent(a)
            this shouldContain isCondition(null, a, stuff)
            this shouldContain containsText(null, a, stuff)
            this shouldContain isPresent(b)
            this shouldContain isCondition(null, b, things)
            this shouldContain containsText(null, b, things)
        }
    }

    @Test
    fun createCondition() {
        val sessionCase = case(a to "1", b to "2")
        with (ConditionSuggester(setOf(a, b), sessionCase)) {
//            val p = this.predicates(tr("5.1"))
//            p shouldHaveSize 4
//            p shouldContain GreaterThanOrEquals(5.1)
//            p shouldContain LessThanOrEquals(5.1)
//            p shouldContain Is("5.1")
//            p shouldContain Contains("5.1")
        }
    }

    @Test
    fun editableValueTest() {
        editableReal(null) shouldBe null
        editableReal(tr("whatever")) shouldBe null
        editableReal(tr("123.99")) shouldBe EditableValue("123.99", Type.Real)
    }
}