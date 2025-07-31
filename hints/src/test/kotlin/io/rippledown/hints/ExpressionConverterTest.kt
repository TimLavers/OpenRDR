package io.rippledown.hints

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExpressionConverterTest {
    private val x = placeHolder
    private val attributes = setOf("ABC", "TSH", "XYZ")

    @Test
    fun `should replace attribute with placeholder`() {
        "ABC is high".insertPlaceholder(attributes) shouldBe Expression("ABC is high", "$x is high", "ABC")
        "elevated TSH".insertPlaceholder(attributes) shouldBe Expression("elevated TSH", "elevated $x", "TSH")
    }

    @Test
    fun `should return blank attribute if the expression does not contain any attribute`() {
        "lowered unknown".insertPlaceholder(attributes) shouldBe Expression("lowered unknown", "lowered unknown")
    }

    @Test
    fun `replacement should be case insensitive`() {
        "abc is high".insertPlaceholder(attributes) shouldBe Expression("abc is high", "$x is high", "ABC")
    }

    @Test
    fun `should replace placeholder with attribute`() {
        removePlaceholder(Expression("abc is high", "$x is high", "ABC")) shouldBe Expression(
            "abc is high",
            "ABC is high"
        )
        removePlaceholder(Expression("elevated TSH", "$x is high", "TSH")) shouldBe Expression(
            "elevated TSH",
            "TSH is high"
        )
        removePlaceholder(Expression("lowered unknown", "lowered unknown")) shouldBe Expression(
            "lowered unknown",
            "lowered unknown"
        )
    }
}