package io.rippledown.model.condition.edit

import io.rippledown.model.condition.Condition
import kotlinx.serialization.Serializable

@Serializable
enum class Type {
    Text {
        override fun convert(value: String): Any? {
            TODO("Not yet implemented")
        }

        override fun valid(value: String): Boolean {
            TODO("Not yet implemented")
        }
    },
    Integer {
        override fun convert(value: String): Any? {
            TODO("Not yet implemented")
        }

        override fun valid(value: String): Boolean {
            TODO("Not yet implemented")
        }
    },
    Real {
        override fun convert(value: String): Double? {
            return value.toDoubleOrNull()
        }

        override fun valid(value: String) = value.toDoubleOrNull() != null
    };

    abstract fun valid(value: String): Boolean
    abstract fun convert(value: String): Any?
}
@Serializable
data class EditableValue(val value: String, val type: Type)

@Serializable
sealed interface EditableCondition {
    fun fixedTextPart1(): String
    fun fixedTextPart2(): String = ""
    fun editableValue(): EditableValue
    fun condition(value: String): Condition
}

