package io.rippledown.expressionparser

import io.rippledown.conditiongenerator.ConditionGenerator
import io.rippledown.llm.conditionSpecificationFor
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

typealias AttributeFor = (String) -> Attribute

class ConditionTip(attributeNames: Collection<String>, attributeFor: AttributeFor) {

    private val expressionConverter = ExpressionConverter(attributeNames)
    private val conditionGenerator = ConditionGenerator(attributeFor)

    fun conditionFor(userText: String): Condition? {
        val expression = expressionConverter.insertPlaceholder(Expression(userText))
        val conditionSpecification = conditionSpecificationFor(expression.text)
        return if (conditionSpecification.predicate.name.isNotBlank()) {
            conditionGenerator.conditionFor(expression.attributeName, userText, conditionSpecification)
        } else {
            null
        }
    }
}