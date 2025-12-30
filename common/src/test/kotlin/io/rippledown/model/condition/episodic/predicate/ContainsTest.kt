package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class ContainsTest : Base() {
    val stuff = "stuff"
    private val contains = Contains(stuff)

    @Test
    fun valueBlank() {
        contains.evaluate(TestResult("")) shouldBe false
    }

    @Test
    fun valueNotBlank() {
        val containsGoat = Contains("goat")
        containsGoat.evaluate(TestResult("")) shouldBe false
        containsGoat.evaluate(TestResult("sheep")) shouldBe false
        containsGoat.evaluate(TestResult("goat")) shouldBe true
        containsGoat.evaluate(TestResult("goats")) shouldBe true
        containsGoat.evaluate(TestResult("sheep and goats")) shouldBe true
        containsGoat.evaluate(TestResult("Goat")) shouldBe false
        containsGoat.evaluate(TestResult("goatherd")) shouldBe true
    }

    @Test
    fun `should strip enclosing double quotes from the text to be matched`() {
        val containsGoat = Contains("\"goat\"")
        containsGoat.evaluate(TestResult("goat")) shouldBe true
        containsGoat.evaluate(TestResult("goatherd")) shouldBe true
    }

    @Test
    fun `should strip enclosing single quotes from the text to be matched`() {
        val containsGoat = Contains("'goat'")
        containsGoat.evaluate(TestResult("goat")) shouldBe true
        containsGoat.evaluate(TestResult("goatherd")) shouldBe true
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
}