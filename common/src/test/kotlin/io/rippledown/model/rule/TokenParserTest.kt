package io.rippledown.model.rule

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlin.test.Test

class TokenParserTest {

    private val weight = Attribute(1, "Weight")
    private val height = Attribute(2, "Height")
    private val riskScore = Attribute(10, "Risk score")

    private fun attributeFor(name: String) = when (name) {
        weight.name -> weight
        height.name -> height
        riskScore.name -> riskScore
        else -> null
    }

    private fun parse(vararg tokens: String) = TokenParser(tokens.toList(), ::attributeFor).parse()

    @Test
    fun `parses a single number`() {
        parse("42") shouldBe Num(42.0)
    }

    @Test
    fun `parses a single attribute`() {
        parse("Weight") shouldBe AttributeValue(weight)
    }

    @Test
    fun `parses an attribute name containing spaces`() {
        parse("Risk score") shouldBe AttributeValue(riskScore)
    }

    @Test
    fun `parses decimal numbers`() {
        parse("3.5") shouldBe Num(3.5)
    }

    @Test
    fun `parses addition`() {
        parse("1", "+", "2") shouldBe Binary(Operator.PLUS, Num(1.0), Num(2.0))
    }

    @Test
    fun `parses subtraction`() {
        parse("5", "-", "3") shouldBe Binary(Operator.MINUS, Num(5.0), Num(3.0))
    }

    @Test
    fun `parses multiplication`() {
        parse("2", "*", "3") shouldBe Binary(Operator.TIMES, Num(2.0), Num(3.0))
    }

    @Test
    fun `parses division`() {
        parse("10", "/", "2") shouldBe Binary(Operator.DIVIDE, Num(10.0), Num(2.0))
    }

    @Test
    fun `multiplication has higher precedence than addition`() {
        parse("1", "+", "2", "*", "3") shouldBe Binary(
            Operator.PLUS,
            Num(1.0),
            Binary(Operator.TIMES, Num(2.0), Num(3.0))
        )
    }

    @Test
    fun `multiplication has higher precedence than subtraction`() {
        parse("10", "-", "2", "*", "3") shouldBe Binary(
            Operator.MINUS,
            Num(10.0),
            Binary(Operator.TIMES, Num(2.0), Num(3.0))
        )
    }

    @Test
    fun `division has higher precedence than addition`() {
        parse("1", "+", "6", "/", "2") shouldBe Binary(
            Operator.PLUS,
            Num(1.0),
            Binary(Operator.DIVIDE, Num(6.0), Num(2.0))
        )
    }

    @Test
    fun `parentheses override precedence`() {
        parse("(", "1", "+", "2", ")", "*", "3") shouldBe Binary(
            Operator.TIMES,
            Binary(Operator.PLUS, Num(1.0), Num(2.0)),
            Num(3.0)
        )
    }

    @Test
    fun `nested parentheses are parsed correctly`() {
        parse("(", "(", "1", "+", "2", ")", "*", "3", ")") shouldBe Binary(
            Operator.TIMES,
            Binary(Operator.PLUS, Num(1.0), Num(2.0)),
            Num(3.0)
        )
    }

    @Test
    fun `addition is left-associative`() {
        parse("1", "+", "2", "+", "3") shouldBe Binary(
            Operator.PLUS,
            Binary(Operator.PLUS, Num(1.0), Num(2.0)),
            Num(3.0)
        )
    }

    @Test
    fun `subtraction is left-associative`() {
        parse("10", "-", "2", "-", "3") shouldBe Binary(
            Operator.MINUS,
            Binary(Operator.MINUS, Num(10.0), Num(2.0)),
            Num(3.0)
        )
    }

    @Test
    fun `multiplication is left-associative`() {
        parse("2", "*", "3", "*", "4") shouldBe Binary(
            Operator.TIMES,
            Binary(Operator.TIMES, Num(2.0), Num(3.0)),
            Num(4.0)
        )
    }

    @Test
    fun `division is left-associative`() {
        parse("100", "/", "5", "/", "2") shouldBe Binary(
            Operator.DIVIDE,
            Binary(Operator.DIVIDE, Num(100.0), Num(5.0)),
            Num(2.0)
        )
    }

    @Test
    fun `parses a mixed attribute and number formula`() {
        parse("Weight", "/", "(", "Height", "*", "Height", ")") shouldBe Binary(
            Operator.DIVIDE,
            AttributeValue(weight),
            Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
        )
    }

    @Test
    fun `parses attribute name containing spaces in a formula`() {
        parse("Risk score", "+", "1") shouldBe Binary(
            Operator.PLUS,
            AttributeValue(riskScore),
            Num(1.0)
        )
    }

    @Test
    fun `returns null for an unknown identifier`() {
        parse("unknown", "+", "1").shouldBeNull()
    }

    @Test
    fun `returns null for missing right operand`() {
        parse("1", "+").shouldBeNull()
    }

    @Test
    fun `returns null for missing left operand`() {
        parse("+", "1").shouldBeNull()
    }

    @Test
    fun `returns null for missing closing parenthesis`() {
        parse("(", "1", "+", "2").shouldBeNull()
    }

    @Test
    fun `returns null for missing opening parenthesis`() {
        parse("1", "+", "2", ")").shouldBeNull()
    }

    @Test
    fun `returns null for empty token list`() {
        TokenParser(emptyList(), ::attributeFor).parse().shouldBeNull()
    }

    @Test
    fun `returns null when there are extra tokens after a complete expression`() {
        parse("1", "2").shouldBeNull()
    }

    @Test
    fun `returns null for a lone operator`() {
        parse("+").shouldBeNull()
    }

    @Test
    fun `returns null for a lone closing parenthesis`() {
        parse(")").shouldBeNull()
    }

    @Test
    fun `returns null for unbalanced parentheses with extra closing paren`() {
        parse("(", "1", "+", "2", ")", ")").shouldBeNull()
    }

    @Test
    fun `returns null for unbalanced parentheses with extra opening paren`() {
        parse("(", "(", "1", "+", "2", ")").shouldBeNull()
    }

    @Test
    fun `returns null for unary minus`() {
        parse("-", "5").shouldBeNull()
    }

    @Test
    fun `returns null for unary plus`() {
        parse("+", "5").shouldBeNull()
    }

    @Test
    fun `returns null for two adjacent operators`() {
        parse("1", "+", "*", "2").shouldBeNull()
    }

    @Test
    fun `returns null for two adjacent operands`() {
        parse("1", "2", "+", "3").shouldBeNull()
    }
}
