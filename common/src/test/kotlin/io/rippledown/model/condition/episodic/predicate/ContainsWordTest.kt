package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class ContainsWordTest : Base() {
    val all = "all"
    private val contains = ContainsWord(all)

    @Test
    fun valueBlank() {
        contains.evaluate(Result("")) shouldBe false
    }

    @Test
    fun evaluateTest() {
        val containsGoat = ContainsWord("all")
        containsGoat.evaluate(Result("")) shouldBe false
        containsGoat.evaluate(Result("or")) shouldBe false
        containsGoat.evaluate(Result("tall")) shouldBe false
        containsGoat.evaluate(Result("gall-bladder")) shouldBe false
        containsGoat.evaluate(Result("not small")) shouldBe false
        containsGoat.evaluate(Result("all")) shouldBe true
        containsGoat.evaluate(Result("all, childhood")) shouldBe true
        containsGoat.evaluate(Result("all; childhood")) shouldBe true
        containsGoat.evaluate(Result("all. childhood")) shouldBe true
        containsGoat.evaluate(Result("all-encompassing")) shouldBe true
        containsGoat.evaluate(Result("not all")) shouldBe true
        containsGoat.evaluate(Result("not all the time")) shouldBe true
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