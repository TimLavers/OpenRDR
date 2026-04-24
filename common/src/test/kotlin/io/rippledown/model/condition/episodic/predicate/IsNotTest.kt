package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Result
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

class IsNotTest : Base() {
    val stuff = "stuff"
    private val predicate = IsNot(stuff)

    @Test
    fun valueBlank() {
        predicate.evaluate(Result("")) shouldBe true
    }

    @Test
    fun valueNotBlank() {
        val isNotGoat = IsNot("goat")
        isNotGoat.evaluate(Result("")) shouldBe true
        isNotGoat.evaluate(Result("sheep")) shouldBe true
        isNotGoat.evaluate(Result("goat")) shouldBe false
        isNotGoat.evaluate(Result("goats")) shouldBe true
        isNotGoat.evaluate(Result("Goat")) shouldBe true
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