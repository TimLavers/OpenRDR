package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class ContainsWordTest : Base() {
    val all = "all"
    private val contains = ContainsWord(all)

    @Test
    fun valueBlank() {
        contains.evaluate(TestResult("")) shouldBe false
    }

    @Test
    fun evaluateTest() {
        val containsGoat = ContainsWord("all")
        containsGoat.evaluate(TestResult("")) shouldBe false
        containsGoat.evaluate(TestResult("or")) shouldBe false
        containsGoat.evaluate(TestResult("tall")) shouldBe false
        containsGoat.evaluate(TestResult("gall-bladder")) shouldBe false
        containsGoat.evaluate(TestResult("not small")) shouldBe false
        containsGoat.evaluate(TestResult("all")) shouldBe true
        containsGoat.evaluate(TestResult("all, childhood")) shouldBe true
        containsGoat.evaluate(TestResult("all; childhood")) shouldBe true
        containsGoat.evaluate(TestResult("all. childhood")) shouldBe true
        containsGoat.evaluate(TestResult("all-encompassing")) shouldBe true
        containsGoat.evaluate(TestResult("not all")) shouldBe true
        containsGoat.evaluate(TestResult("not all the time")) shouldBe true
    }

    @Test
    fun equalsTest() {
        ContainsWord("Blah") shouldBe ContainsWord("Blah")
        ContainsWord("Blah") shouldNotBe ContainsWord("blah")
        ContainsWord("Blah") shouldNotBe ContainsWord("")
    }

    @Test
    fun hashCodeTest() {
        ContainsWord("Blah").hashCode() shouldBe ContainsWord("Blah").hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(contains) shouldBe contains
    }

    @Test
    fun description() {
        contains.description(false) shouldBe "contains word \"$all\""
        contains.description(true) shouldBe "contain word \"$all\""
    }

    @Test
    fun `description should not double-quote when toFind already has double quotes`() {
        val containsQuoted = ContainsWord("\"2\"")
        containsQuoted.description(false) shouldBe "contains word \"2\""
        containsQuoted.description(true) shouldBe "contain word \"2\""
    }

    @Test
    fun `description should not double-quote when toFind already has single quotes`() {
        val containsQuoted = ContainsWord("'2'")
        containsQuoted.description(false) shouldBe "contains word \"2\""
        containsQuoted.description(true) shouldBe "contain word \"2\""
    }
}