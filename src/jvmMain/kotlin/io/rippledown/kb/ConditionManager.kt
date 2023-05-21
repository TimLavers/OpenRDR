package io.rippledown.kb

import io.rippledown.model.RDRCase
import io.rippledown.model.condition.ConditionList
import io.rippledown.model.condition.HasCurrentValue

class ConditionManager {
    fun conditionHintsForCase(case: RDRCase): ConditionList {
        val conditions = case.attributes.map { attribute ->
            HasCurrentValue(attribute)
        }.filter {
            it.holds(case)
        }
        return ConditionList(conditions)
    }
}