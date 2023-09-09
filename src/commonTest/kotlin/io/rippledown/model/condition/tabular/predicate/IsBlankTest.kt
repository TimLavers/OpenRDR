package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlin.test.Test

class IsBlankTest: Base() {
    private val blank = IsBlank

    @Test
    fun valueBlank() {
        blank.evaluate(TestResult("")) shouldBe true
    }

    @Test
    fun valueNotBlank() {
        blank.evaluate(TestResult("whatever")) shouldBe false
    }

    @Test
    fun whitespace() {
        // Not sure what to do here. Should blank values be allowed?
        blank.evaluate(TestResult(" ")) shouldBe true
    }

    @Test
    fun equalsTest() {
        blank shouldBe IsBlank
    }

    @Test
    fun hashCodeTest() {
        blank.hashCode() shouldBe IsBlank.hashCode()
    }

    @Test
    fun serialization() {
        serializeDeserialize(blank) shouldBe blank
    }

    @Test
    fun description() {
        blank.description(false) shouldBe "is blank"
        blank.description(true) shouldBe "are blank"
    }
}