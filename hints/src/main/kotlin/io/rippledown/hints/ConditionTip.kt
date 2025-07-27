package io.rippledown.hints

import io.rippledown.hints.ConditionService.conditionSpecificationsFor
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

typealias AttributeFor = (String) -> Attribute

class ConditionTip(attributeNames: Collection<String>, attributeFor: AttributeFor) {
    private val logger = lazyLogger
    private val expressionConverter = ExpressionConverter(attributeNames)
    private val conditionGenerator = ConditionGenerator(attributeFor)

    fun conditionFor(userText: String) = conditionsFor(listOf(userText))[0]

    fun conditionsFor(userTexts: List<String>): List<Condition?> {
        if (userTexts.isEmpty()) return emptyList()
        val nonBlankTexts = userTexts.map { if (it.isBlank()) "" else it }
        return try {
            val expressions = nonBlankTexts.map { expressionConverter.insertPlaceholder(Expression(it)) }
            val conditionSpecs = conditionSpecificationsFor(*expressions.map { it.text }.toTypedArray())
            userTexts.zip(expressions).zip(conditionSpecs).map { (userTextAndExpression, spec) ->
                val (userText, expression) = userTextAndExpression
                when {
                    userText.isBlank() -> null
                    spec.predicate.name.isNotBlank() -> conditionGenerator.conditionFor(
                        expression.attributeName,
                        userText,
                        spec
                    )

                    else -> null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create conditions for texts: '$userTexts'", e)
            userTexts.map { null }
        }
    }
}