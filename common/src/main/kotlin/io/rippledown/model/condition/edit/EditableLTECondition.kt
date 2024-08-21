package io.rippledown.model.condition.edit

import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.predicate.LessThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.serialization.Serializable

@Serializable
data class EditableLTECondition(val attribute: Attribute, val initialCutoff: EditableValue): EditableCondition {

    override fun fixedTextPart1(): String {
        return "${attribute.name} ≤ "
    }

    override fun fixedTextPart2(): String {
        return ""
    }

    override fun editableValue(): EditableValue {
        return initialCutoff
    }

    override fun condition(value: String): Condition {
        require(initialCutoff.type.valid(value))
        val cutoff = initialCutoff.type.convert(value) as Double
        return EpisodicCondition(attribute, LessThanOrEquals(cutoff), Current)
    }
}