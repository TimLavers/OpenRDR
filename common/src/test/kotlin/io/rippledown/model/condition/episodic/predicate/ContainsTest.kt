package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class ContainsTest : Base() {
    val stuff = "stuff"
    private val contains = Contains(stuff)

    @Test
    fun valueBlank() {
        contains.evaluate(Result("")) shouldBe false
    }

    @Test
    fun valueNotBlank() {
        val containsGoat = Contains("goat")
        containsGoat.evaluate(Result("")) shouldBe false
        containsGoat.evaluate(Result("sheep")) shouldBe false
        containsGoat.evaluate(Result("goat")) shouldBe true
        containsGoat.evaluate(Result("goats")) shouldBe true
        containsGoat.evaluate(Result("sheep and goats")) shouldBe true
        containsGoat.evaluate(Result("Goat")) shouldBe false
        containsGoat.evaluate(Result("goatherd")) shouldBe true
    }

    @Test
    fun `should strip enclosing double quotes from the text to be matched`() {
        val containsGoat = Contains("\"goat\"")
        containsGoat.evaluate(Result("goat")) shouldBe true
        containsGoat.evaluate(Result("goatherd")) shouldBe true
    }

    @Test
    fun `should strip enclosing single quotes from the text to be matched`() {
        val containsGoat = Contains("'goat'")
        containsGoat.evaluate(Result("goat")) shouldBe true
        containsGoat.evaluate(Result("goatherd")) shouldBe true
    }

    @Test
    fun `should should match a string with a forward slash`() {
        Contains("/40").evaluate(Result("12/40")) shouldBe true
    }

    @Test
    fun equalsTest() {
        Contains("Blah") shouldBe Contains("Blah")
        Contains("Blah") shouldNotBe Contains("blah")
        Contains("Blah") shouldNotBe Contains("")
    }

    @Test
    fun hashCodeTest() {
        Contains("Blah").hashCode() shouldBe Contains("Blah").hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(contains) shouldBe contains
    }

    @Test
    fun description() {
        contains.description(false) shouldBe "contains \"$stuff\""
        contains.description(true) shouldBe "contain \"$stuff\""
    }

    @Test
    fun `description should not double-quote when toFind already has double quotes`() {
        val containsQuoted = Contains("\"2\"")
        containsQuoted.description(false) shouldBe "contains \"2\""
        containsQuoted.description(true) shouldBe "contain \"2\""
    }

    @Test
    fun `description should not double-quote when toFind already has single quotes`() {
        val containsQuoted = Contains("'2'")
        containsQuoted.description(false) shouldBe "contains \"2\""
        containsQuoted.description(true) shouldBe "contain \"2\""
    }

}