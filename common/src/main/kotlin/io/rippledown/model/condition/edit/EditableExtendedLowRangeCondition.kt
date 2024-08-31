package io.rippledown.model.condition.edit

import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.edit.Type.Integer
import io.rippledown.model.condition.episodic.predicate.ExtendedRangeFunction
import io.rippledown.model.condition.episodic.predicate.LowByAtMostSomePercentage
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.serialization.Serializable

@Serializable
data class EditableExtendedLowRangeCondition(val attribute: Attribute): EditableCondition {
    private val initialLimit: EditableValue = EditableValue("10", Integer)

    override fun fixedTextPart1() = condition("1").asText().dropLast(2)

    override fun fixedTextPart2(): String {
        return "%"
    }

    override fun editableValue(): EditableValue {
        return initialLimit
    }

    override fun condition(value: String): Condition {
        require(Integer.valid(value))
        val limit = Integer.convert(value) as Int
        return EpisodicCondition(attribute, LowByAtMostSomePercentage(limit), Current)
    }
}