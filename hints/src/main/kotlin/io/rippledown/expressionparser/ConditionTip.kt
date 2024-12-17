package io.rippledown.expressionparser

import io.rippledown.conditiongenerator.ConditionGenerator
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

typealias AttributeFor = (String) -> Attribute

class ConditionTip(attributeNames: Collection<String>, attributeFor: AttributeFor) {

    private val expressionConverter = ExpressionConverter(attributeNames)
    private val conditionGenerator = ConditionGenerator(attributeFor)

    fun conditionFor(userText: String): Condition? {
        val expression = expressionConverter.insertPlaceholder(Expression(userText))

//        val tokens = tokensFor(expression.text)
//        return conditionGenerator.conditionFor(expression.attribute, userText, *tokens)
        return null
    }

}