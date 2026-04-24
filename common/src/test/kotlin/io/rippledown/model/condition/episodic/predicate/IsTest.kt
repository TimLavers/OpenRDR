package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IsTest : Base() {
    val stuff = "stuff"
    private val predicate = Is(stuff)

    @Test
    fun valueBlank() {
        predicate.evaluate(Result("")) shouldBe false
    }

    @Test
    fun valueNotBlank() {
        val isGoat = Is("goat")
        isGoat.evaluate(Result("")) shouldBe false
        isGoat.evaluate(Result("sheep")) shouldBe false
        isGoat.evaluate(Result("goat")) shouldBe true
        isGoat.evaluate(Result("goats")) shouldBe false
        isGoat.evaluate(Result("Goat")) shouldBe false
    }

    @Test
    fun `evaluation should ignore surrounding quotesin the value being compared with`() {
        val isGoat = Is("goat")
        isGoat.evaluate(Result("\"goat\"")) shouldBe true
    }

    @Test
    fun `evaluation should ignore surrounding quotes in the condition parameter`() {
        val isGoat = Is("\"goat\"")
        isGoat.evaluate(Result("goat")) shouldBe true
    }

    @Test
    fun `evaluation should ignore surrounding apostrophes in the value being compared with`() {
        val isGoat = Is("goat")
        isGoat.evaluate(Result("'goat'")) shouldBe true
    }

    @Test
    fun `evaluation should ignore surrounding apostrophes in the condition parameter`() {
        val isGoat = Is("'goat'")
        isGoat.evaluate(Result("goat")) shouldBe true
    }

    @Test
    fun equalsTest() {
        Is("Blah") shouldBe Is("Blah")
        Is("Blah") shouldNotBe Is("blah")
        Is("Blah") shouldNotBe Is("")
    }

    @Test
    fun hashCodeTest() {
        Is("Blah").hashCode() shouldBe Is("Blah").hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(predicate) shouldBe predicate
    }

    @Test
    fun description() {
        predicate.description(false) shouldBe "is \"$stuff\""
        predicate.description(true) shouldBe "are \"$stuff\""
    }

    @Test
    fun `description should show one set of quotes`() {
        Is("goat").description(false) shouldBe "is \"goat\""
        Is("\"goat\"").description(false) shouldBe "is \"goat\""
        Is("'goat\'").description(false) shouldBe "is \"goat\""
    }
}