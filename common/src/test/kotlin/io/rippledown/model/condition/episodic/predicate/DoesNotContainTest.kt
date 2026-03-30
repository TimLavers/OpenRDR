package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class DoesNotContainTest : Base() {
    val stuff = "stuff"
    private val dnc = DoesNotContain(stuff)

    @Test
    fun valueBlank() {
        dnc.evaluate(TestResult("")) shouldBe true
    }

    @Test
    fun valueNotBlank() {
        val noGoat = DoesNotContain("goat")
        noGoat.evaluate(TestResult("")) shouldBe true
        noGoat.evaluate(TestResult("sheep")) shouldBe true
        noGoat.evaluate(TestResult("goat")) shouldBe false
        noGoat.evaluate(TestResult("goats")) shouldBe false
        noGoat.evaluate(TestResult("Goat")) shouldBe true
    }

    @Test
    fun `should should match a string with a forward slash`() {
        DoesNotContain("/40").evaluate(TestResult("12/40")) shouldBe false
    }

    @Test
    fun `should unquote toFind before evaluating`() {
        DoesNotContain("\"/40\"").evaluate(TestResult("12/40")) shouldBe false
    }
    @Test
    fun equalsTest() {
        DoesNotContain("Blah") shouldBe DoesNotContain("Blah")
        DoesNotContain("Blah") shouldNotBe DoesNotContain("blah")
        DoesNotContain("Blah") shouldNotBe DoesNotContain("")
    }

    @Test
    fun hashCodeTest() {
        DoesNotContain("Blah").hashCode() shouldBe DoesNotContain("Blah").hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(dnc) shouldBe dnc
    }

    @Test
    fun description() {
        dnc.description(false) shouldBe "does not contain \"$stuff\""
        dnc.description(true) shouldBe "do not contain \"$stuff\""
    }

    @Test
    fun `description should not double-quote when toFind already has double quotes`() {
        val dncQuoted = DoesNotContain("\"2\"")
        dncQuoted.description(false) shouldBe "does not contain \"2\""
        dncQuoted.description(true) shouldBe "do not contain \"2\""
    }

    @Test
    fun `description should not double-quote when toFind already has single quotes`() {
        val dncQuoted = DoesNotContain("'2'")
        dncQuoted.description(false) shouldBe "does not contain \"2\""
        dncQuoted.description(true) shouldBe "do not contain \"2\""
    }
}