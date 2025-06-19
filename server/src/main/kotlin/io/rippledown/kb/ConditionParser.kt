package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition

interface ConditionParser {
    fun parse(expression: String, attributeFor: (String) -> Attribute): Condition? = null
}