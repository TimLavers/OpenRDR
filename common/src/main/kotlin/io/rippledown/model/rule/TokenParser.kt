package io.rippledown.model.rule

import io.rippledown.model.Attribute

/**
 * Recursive descent parser for a tokenised formula expression.
 *
 * Parses the grammar:
 *   expression = term { ('+' | '-') term }
 *   term       = factor { ('*' | '/') factor }
 *   factor     = number | attribute | '(' expression ')'
 *
 * Returns null if the token stream is not a well-formed formula or if an
 * identifier cannot be resolved to an attribute.
 */
internal class TokenParser(
    private val tokens: List<String>,
    private val attributeFor: (String) -> Attribute?
) {
    private var position = 0

    fun parse(): Expr? {
        val expr = parseExpression() ?: return null
        return if (isAtEnd()) expr else null
    }

    private fun parseExpression(): Expr? {
        var left = parseTerm() ?: return null
        while (peek() == "+" || peek() == "-") {
            val operator = if (next() == "+") Operator.PLUS else Operator.MINUS
            val right = parseTerm() ?: return null
            left = Binary(operator, left, right)
        }
        return left
    }

    private fun parseTerm(): Expr? {
        var left = parseFactor() ?: return null
        while (peek() == "*" || peek() == "/") {
            val operator = if (next() == "*") Operator.TIMES else Operator.DIVIDE
            val right = parseFactor() ?: return null
            left = Binary(operator, left, right)
        }
        return left
    }

    private fun parseFactor(): Expr? {
        val token = peek() ?: return null
        if (token == "(") {
            next()
            val expr = parseExpression() ?: return null
            if (next() != ")") return null
            return expr
        }
        next()
        token.toDoubleOrNull()?.let { return Num(it) }
        return attributeFor(token)?.let { AttributeValue(it) }
    }

    private fun isAtEnd() = position == tokens.size
    private fun peek() = tokens.getOrNull(position)
    private fun next() = tokens.getOrNull(position++)
}
