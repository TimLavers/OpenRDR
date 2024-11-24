package io.rippledown.expressionparser

import io.kotest.matchers.shouldBe
import io.rippledown.expressionparser.ExpressionConverter.Companion.placeHolder
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ExpressionConverterTest {
    private lateinit var expressionConverter: ExpressionConverter
    private val x = placeHolder

    @BeforeEach
    fun setUp() {
        expressionConverter = ExpressionConverter(setOf("ABC", "TSH", "XYZ"))
    }

    @Test
    fun `should replace attribute with placeholder`() {
        expressionConverter.insertPlaceholder(Expression("ABC is high")) shouldBe Expression("$x is high", "ABC")
        expressionConverter.insertPlaceholder(Expression("elevated TSH")) shouldBe Expression("elevated $x", "TSH")
    }

    @Test
    fun `should return blank attribute if the expression does not contain any attribute`() {
        expressionConverter.insertPlaceholder(Expression("lowered unknown")) shouldBe Expression("lowered unknown")
    }

    @Test
    fun `replacement should be case insensitive`() {
        expressionConverter.insertPlaceholder(Expression("abc is high")) shouldBe Expression("$x is high", "ABC")
        expressionConverter.insertPlaceholder(Expression("elevated tsh")) shouldBe Expression("elevated $x", "TSH")
        expressionConverter.insertPlaceholder(Expression("Lowered Unknown")) shouldBe Expression("Lowered Unknown")
    }

    @Test
    fun `should replace placeholder with attribute`() {
        expressionConverter.removePlaceholder(Expression("$x is high", "ABC")) shouldBe Expression("ABC is high")
        expressionConverter.removePlaceholder(Expression("elevated $x", "TSH")) shouldBe Expression("elevated TSH")
        expressionConverter.removePlaceholder(Expression("lowered unknown")) shouldBe Expression("lowered unknown")
    }
}