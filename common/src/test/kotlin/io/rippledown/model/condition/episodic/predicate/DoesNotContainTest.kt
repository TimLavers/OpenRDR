package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.model.serializeDeserialize
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
}