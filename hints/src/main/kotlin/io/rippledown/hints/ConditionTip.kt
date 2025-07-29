package io.rippledown.hints

import io.rippledown.hints.ConditionService.conditionSpecificationsFor
import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

typealias AttributeFor = (String) -> Attribute

class ConditionTip(val attributeNames: Collection<String>, attributeFor: AttributeFor) {
    private val logger = lazyLogger
    private val conditionGenerator = ConditionGenerator(attributeFor)

    fun conditionFor(userText: String) = conditionsFor(listOf(userText))[0]

    fun conditionsFor(userTexts: List<String>): List<Condition?> {
        if (userTexts.isEmpty()) return emptyList()
        val nonBlankTexts = userTexts.map { if (it.isBlank()) "" else it }
        return try {
            val expressions = nonBlankTexts.map { it.insertPlaceholder(attributeNames = attributeNames) }
            val conditionSpecs = conditionSpecificationsFor(*expressions.map { it.textWithPlaceholder }.toTypedArray())
            expressions.zip(conditionSpecs).map { (expression, spec) ->
                conditionGenerator.conditionFor(
                        expression.attributeName,
                    expression.originalText,
                        spec
                    )

                }
        } catch (e: Exception) {
            logger.error("Failed to create conditions for texts: '$userTexts'", e)
            userTexts.map { null }
        }
    }
}