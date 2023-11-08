package io.rippledown.model.condition.episodic.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.TestResult
import kotlin.test.Test

class IsNotBlankTest: Base() {
    private val notBlank = IsNotBlank

    @Test
    fun valueBlank() {
        notBlank.evaluate(TestResult("")) shouldBe false
    }

    @Test
    fun valueNotBlank() {
        notBlank.evaluate(TestResult("whatever")) shouldBe true
    }

    @Test
    fun whitespace() {
        // Not sure what to do here. Should blank values be allowed?
        notBlank.evaluate(TestResult(" ")) shouldBe false
    }

    @Test
    fun equalsTest() {
        notBlank shouldBe IsNotBlank
    }

    @Test
    fun hashCodeTest() {
        notBlank.hashCode() shouldBe IsNotBlank.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(notBlank) shouldBe notBlank
    }

    @Test
    fun description() {
        notBlank.description(false) shouldBe "is not blank"
        notBlank.description(true) shouldBe "are not blank"
    }
}