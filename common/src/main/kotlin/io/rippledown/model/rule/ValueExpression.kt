package io.rippledown.model.rule

import io.rippledown.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.MathContext

/**
 * The value assigned to a derived attribute by an [AssignValue] rule action.
 * Either a literal, or an arithmetic formula over the latest values of
 * other attributes. See documentation/design/repeat_inferencing.md.
 */
@Serializable
sealed class ValueExpression {
    /**
     * The value for the given case, or null if no assignment should be
     * made.
     */
    abstract fun evaluate(case: RDRCase): String?

    abstract fun referencedAttributes(): Set<Attribute>

    abstract fun asText(): String

    abstract fun alignAttributes(idToAttribute: (Int) -> Attribute): ValueExpression
}

@Serializable
@SerialName("Literal")
data class Literal(val value: String) : ValueExpression() {
    override fun evaluate(case: RDRCase) = value
    override fun referencedAttributes() = emptySet<Attribute>()
    override fun asText() = "\"$value\""
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = this
}

/**
 * An arithmetic formula over the latest values of other attributes.
 * Arithmetic needs numbers, so the formula evaluates to null — and no
 * assignment is made — if any attribute it references is absent from the
 * case or has a value that does not parse as a number. This mirrors the
 * way numeric conditions do not hold for non-numeric values, and ensures
 * that a partial case never yields a partial derived value.
 */
@Serializable
@SerialName("Formula")
data class Formula(val expression: Expr) : ValueExpression() {
    override fun evaluate(case: RDRCase) = expression.evaluate(case)?.toValueString()
    override fun referencedAttributes() = expression.referencedAttributes()
    override fun asText() = expression.asText()
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = Formula(expression.alignAttributes(idToAttribute))
}

/**
 * A sentinel expression indicating that the value assigned to a derived
 * attribute is given by the attribute's stored definition, rather than by
 * an expression embedded in the rule action. It is never evaluated
 * directly: before evaluation it is resolved to the concrete expression
 * held in the definition store. See
 * documentation/design/editing_derived_attribute_definitions.md.
 */
@Serializable
@SerialName("ByDefinition")
object ByDefinition : ValueExpression() {
    override fun evaluate(case: RDRCase): String? =
        error("A ByDefinition expression must be resolved to a concrete expression before evaluation.")

    override fun referencedAttributes(): Set<Attribute> =
        error("A ByDefinition expression must be resolved to a concrete expression before its referenced attributes can be determined.")

    override fun asText() = "by definition"

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = this
}

/**
 * The value of a comment attribute: a text template whose positional
 * `${}` tokens are substituted with the latest case values of the
 * corresponding [variables]. This carries over the rendering semantics of
 * `Conclusion`: a variable that is missing from the case or blank renders
 * as a `{name: no value}` marker, whose position is reported by [render]
 * so that the UI can highlight it. See "Phase 2 — comments become derived
 * attributes" in documentation/design/repeat_inferencing.md.
 */
@Serializable
@SerialName("CommentTemplate")
data class CommentTemplate(
    val text: String,
    val variables: List<Attribute> = emptyList()
) : ValueExpression() {
    override fun evaluate(case: RDRCase): String = render(case).text

    override fun referencedAttributes() = variables.toSet()

    override fun asText() = "\"${substituteTokens { "{${it.name}}" }}\""

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) =
        CommentTemplate(text, variables.map { idToAttribute(it.id) })

    /**
     * The template text with each `${}` token replaced by its variable's
     * name in `{attributeName}` format — the form in which comments with
     * variables are presented to the LLM. The attribute name is resolved
     * through [attributeById] where possible (the stored variable may
     * carry a stale name), falling back to the variable's own name.
     */
    fun textWithVariableNames(attributeById: (Int) -> Attribute? = { null }): String =
        substituteTokens { "{${(attributeById(it.id) ?: it).name}}" }

    /**
     * The comment for the given case, with the ranges of any unresolved
     * variable markers.
     */
    fun render(case: RDRCase): RenderedComment {
        if (variables.isEmpty()) {
            return RenderedComment(text, emptyList())
        }
        val builder = StringBuilder()
        val unresolvedRanges = mutableListOf<IntRangeData>()
        var textPosition = 0
        variables.forEach { attribute ->
            val tokenIndex = text.indexOf(VARIABLE_TOKEN, textPosition)
            if (tokenIndex != -1) {
                builder.append(text.substring(textPosition, tokenIndex))
                val value = if (case.dates.isNotEmpty()) case.latestValue(attribute) else null
                if (value != null && value.isNotBlank()) {
                    builder.append(value)
                } else {
                    val marker = "{${attribute.name}: no value}"
                    val markerStart = builder.length
                    builder.append(marker)
                    unresolvedRanges.add(IntRangeData(markerStart, builder.length - 1))
                }
                textPosition = tokenIndex + VARIABLE_TOKEN.length
            }
        }
        if (textPosition < text.length) {
            builder.append(text.substring(textPosition))
        }
        return RenderedComment(builder.toString(), unresolvedRanges)
    }

    /**
     * Replace each `${}` token (in order of appearance) with the given
     * rendering of the corresponding variable.
     */
    private fun substituteTokens(renderVariable: (Attribute) -> String): String {
        val builder = StringBuilder()
        var pos = 0
        var varIndex = 0
        while (pos < text.length) {
            val tokenIndex = text.indexOf(VARIABLE_TOKEN, pos)
            if (tokenIndex == -1 || varIndex >= variables.size) {
                builder.append(text.substring(pos))
                break
            }
            builder.append(text.substring(pos, tokenIndex))
            builder.append(renderVariable(variables[varIndex]))
            pos = tokenIndex + VARIABLE_TOKEN.length
            varIndex++
        }
        return builder.toString()
    }
}

/**
 * A node in the arithmetic syntax tree of a [Formula].
 */
@Serializable
sealed class Expr {
    /**
     * The numeric value for the given case, or null if it cannot be
     * computed (a referenced attribute is absent or non-numeric, or a
     * division by zero).
     */
    abstract fun evaluate(case: RDRCase): Double?
    abstract fun referencedAttributes(): Set<Attribute>
    abstract fun asText(): String
    abstract fun alignAttributes(idToAttribute: (Int) -> Attribute): Expr
}

@Serializable
@SerialName("Number")
data class Num(val value: Double) : Expr() {
    override fun evaluate(case: RDRCase) = value
    override fun referencedAttributes() = emptySet<Attribute>()
    override fun asText() = value.toValueString()
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = this
}

@Serializable
@SerialName("AttributeValue")
data class AttributeValue(val attribute: Attribute) : Expr() {
    override fun evaluate(case: RDRCase): Double? {
        if (!case.attributes.contains(attribute)) return null
        return case.latestValue(attribute)?.toDoubleOrNull()
    }

    override fun referencedAttributes() = setOf(attribute)
    override fun asText() = attribute.name
    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = AttributeValue(idToAttribute(attribute.id))
}

@Serializable
@SerialName("Binary")
data class Binary(val operator: Operator, val left: Expr, val right: Expr) : Expr() {
    override fun evaluate(case: RDRCase): Double? {
        val leftValue = left.evaluate(case) ?: return null
        val rightValue = right.evaluate(case) ?: return null
        val result = operator.apply(leftValue, rightValue)
        return if (result.isFinite()) result else null
    }

    override fun referencedAttributes() = left.referencedAttributes() + right.referencedAttributes()

    override fun asText(): String {
        val leftText =
            if (left is Binary && left.operator.precedence < operator.precedence) "(${left.asText()})" else left.asText()
        val rightText =
            if (right is Binary && right.operator.precedence <= operator.precedence) "(${right.asText()})" else right.asText()
        return "$leftText ${operator.symbol} $rightText"
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) =
        Binary(operator, left.alignAttributes(idToAttribute), right.alignAttributes(idToAttribute))
}

@Serializable
enum class Operator(val symbol: String, val precedence: Int) {
    PLUS("+", 1) {
        override fun apply(left: Double, right: Double) = left + right
    },
    MINUS("-", 1) {
        override fun apply(left: Double, right: Double) = left - right
    },
    TIMES("*", 2) {
        override fun apply(left: Double, right: Double) = left * right
    },
    DIVIDE("/", 2) {
        override fun apply(left: Double, right: Double) = left / right
    },
    POWER("**", 3) {
        override fun apply(left: Double, right: Double) = Math.pow(left, right)
    };

    abstract fun apply(left: Double, right: Double): Double
}

/**
 * The default number of significant figures used when displaying a
 * derived attribute value. Repeated evaluation is deterministic because
 * the same precision is used on every computation path.
 */
internal const val DERIVED_VALUE_SIGNIFICANT_FIGURES = 4

/**
 * Render a numeric value as an assigned value string: whole numbers
 * without a decimal point, others to [DERIVED_VALUE_SIGNIFICANT_FIGURES]
 * significant digits.
 */
internal fun Double.toValueString(): String {
    val rounded = toBigDecimal(MathContext(DERIVED_VALUE_SIGNIFICANT_FIGURES)).stripTrailingZeros()
    return rounded.toPlainString()
}

/**
 * The value expression for the given user-entered text. The text is a
 * formula if it parses as arithmetic whose identifiers all resolve to
 * attributes; otherwise it is a literal. Values are typed by example:
 * a plain number is a numeric literal.
 */
fun parseValueExpression(text: String, attributeFor: (String) -> Attribute?): ValueExpression {
    val trimmed = text.trim()
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
        return Literal(trimmed.substring(1, trimmed.length - 1))
    }
    if (trimmed.toDoubleOrNull() != null) return Literal(trimmed)
    val expr = FormulaParser(attributeFor).parse(trimmed)
    return if (expr != null && expr.referencedAttributes().isNotEmpty()) Formula(expr) else Literal(trimmed)
}

/**
 * Recursive descent parser for the formula language: `+ - * /`,
 * parentheses, numeric literals, and attribute names (which may contain
 * spaces). Returns null from [parse] if the text is not a well-formed
 * formula or an identifier does not resolve to an attribute.
 */
class FormulaParser(private val attributeFor: (String) -> Attribute?) {

    fun parse(text: String): Expr? {
        val tokens = tokenize(text) ?: return null
        return TokenParser(tokens, attributeFor).parse()
    }

    private fun tokenize(text: String): List<String>? {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            val token = current.toString().trim()
            if (token.isNotEmpty()) tokens.add(token)
            current.clear()
        }
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when (c) {
                '+', '-', '/', '(', ')' -> {
                    flush()
                    tokens.add(c.toString())
                }

                '*' -> {
                    flush()
                    if (i + 1 < text.length && text[i + 1] == '*') {
                        tokens.add("**")
                        i++
                    } else {
                        tokens.add("*")
                    }
                }

                else -> current.append(c)
            }
            i++
        }
        flush()
        return tokens.ifEmpty { null }
    }
}
