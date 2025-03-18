package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.TestResult
import io.rippledown.model.serializeDeserialize
import kotlin.test.Test

class IsNotTest : Base() {
    val stuff = "stuff"
    private val predicate = IsNot(stuff)

    @Test
    fun valueBlank() {
        predicate.evaluate(TestResult("")) shouldBe true
    }

    @Test
    fun valueNotBlank() {
        val isNotGoat = IsNot("goat")
        isNotGoat.evaluate(TestResult("")) shouldBe true
        isNotGoat.evaluate(TestResult("sheep")) shouldBe true
        isNotGoat.evaluate(TestResult("goat")) shouldBe false
        isNotGoat.evaluate(TestResult("goats")) shouldBe true
        isNotGoat.evaluate(TestResult("Goat")) shouldBe true
    }

    @Test
    fun equalsTest() {
        IsNot("Blah") shouldBe IsNot("Blah")
        IsNot("Blah") shouldNotBe IsNot("blah")
        IsNot("Blah") shouldNotBe IsNot("")
    }

    @Test
    fun hashCodeTest() {
        IsNot("Blah").hashCode() shouldBe IsNot("Blah").hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(predicate) shouldBe predicate
    }

    @Test
    fun description() {
        predicate.description(false) shouldBe "is not \"$stuff\""
        predicate.description(true) shouldBe "are not \"$stuff\""
    }
}