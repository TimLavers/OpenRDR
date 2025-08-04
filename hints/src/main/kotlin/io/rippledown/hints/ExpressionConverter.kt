package io.rippledown.hints

const val placeHolder = "x"

/**
 * Replace each instance of an attribute name in the expression with a placeholder.
 * Note. We assume there is only one attribute name in the expression, even though there may be multiple instances of it.
 * @return an Expression with the original text, text with placeholder, and matched attribute name.
 */
fun String.insertPlaceholder(attributeNames: Collection<String>): Expression {
    val originalText = this
    attributeNames.forEach { attributeName ->
        if (originalText.contains(attributeName, ignoreCase = true)) {
            val textWithPlaceholder = originalText.replace(attributeName, placeHolder, ignoreCase = true)
            return Expression(originalText, textWithPlaceholder, attributeName)
        }
    }
    return Expression(originalText, originalText, "")
}

fun removePlaceholder(expression: Expression) = with(expression) {
    Expression(originalText, textWithPlaceholder.replace(placeHolder, attributeName, ignoreCase = true))
}


data class Expression(val originalText: String, val textWithPlaceholder: String, val attributeName: String = "")