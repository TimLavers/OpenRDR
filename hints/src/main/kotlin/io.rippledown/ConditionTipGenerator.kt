package io.rippledown

class ConditionTipGenerator(attributeNames: Set<String>) {

    private val expressionConverter = ExpressionConverter(attributeNames)

    fun conditionTip(userText: String): String {
        val expression = expressionConverter.insertPlaceholder(Expression(userText))
        val suggestion = Expression(suggestionFor(expression.text), expression.attribute)
        return expressionConverter.removePlaceholder(suggestion).text
    }

}