package io.rippledown.model.condition.edit

import io.rippledown.model.Attribute
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.GreaterThanOrEquals
import io.rippledown.model.condition.episodic.signature.Current
import kotlinx.serialization.Serializable

@Serializable
enum class Type {
    Text {
        override fun valid(value: String): Boolean {
            TODO("Not yet implemented")
        }
    },
    Integer {
        override fun valid(value: String): Boolean {
            TODO("Not yet implemented")
        }
    },
    Real {
        override fun valid(value: String): Boolean {
            TODO("Not yet implemented")
        }
    };

    abstract fun valid(value: String): Boolean
}
@Serializable
data class EditableValue(val value: String, val type: Type)

@Serializable
sealed interface EditableCondition {
    fun fixedTextPart1(): String
    fun fixedTextPart2(): String
    fun editableValue(): EditableValue
    fun condition(value: String): Condition
}
@Serializable
data class EditableGTECondition(val attribute: Attribute,
                             val initialEditableValue: EditableValue): EditableCondition {

    override fun fixedTextPart1(): String {
        return "${attribute.name} ≥ "
    }

    override fun fixedTextPart2(): String {
        return ""
    }

    override fun editableValue(): EditableValue {
        return initialEditableValue
    }

    override fun condition(value: String): Condition {
        return EpisodicCondition(attribute, GreaterThanOrEquals(value.toDouble()), Current)
    }
}