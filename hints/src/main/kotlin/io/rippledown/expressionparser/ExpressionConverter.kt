package io.rippledown.expressionparser

class ExpressionConverter(private val attributeNames: Collection<String>) {

    fun insertPlaceholder(expression: Expression) = attributeNames.fold(expression) { acc, attributeName ->
        if (acc.text.contains(attributeName, ignoreCase = true)) {
            Expression(acc.text.replace(attributeName, placeHolder, ignoreCase = true), attributeName)
        } else {
            acc
        }
    }

    fun removePlaceholder(expression: Expression) = with(expression) {
        Expression(text.replace(placeHolder, attributeName), "")
    }

    companion object {
        val placeHolder = "x"
    }
}

data class Expression(val text: String, val attributeName: String = "")