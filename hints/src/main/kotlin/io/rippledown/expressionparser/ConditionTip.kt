package io.rippledown.expressionparser

import io.rippledown.conditiongenerator.ConditionGenerator
import io.rippledown.llm.conditionSpecificationFor
import io.rippledown.logger
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

typealias AttributeFor = (String) -> Attribute

class ConditionTip(attributeNames: Collection<String>, attributeFor: AttributeFor) {
    private val logger = ConditionTip::class.logger()
    private val expressionConverter = ExpressionConverter(attributeNames)
    private val conditionGenerator = ConditionGenerator(attributeFor)

    fun conditionFor(userText: String): Condition? {
        if (userText.isBlank()) return null // Early return for invalid input

        return try {
            val expression = expressionConverter.insertPlaceholder(Expression(userText))
            val conditionSpec = conditionSpecificationFor(expression.text)

            when {
                conditionSpec.predicate.name.isNotBlank() -> {
                    conditionGenerator.conditionFor(
                        expression.attributeName,
                        userText,
                        conditionSpec
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            logger.error("Failed to create condition for text: '$userText'", e)
            null
        }
    }
}