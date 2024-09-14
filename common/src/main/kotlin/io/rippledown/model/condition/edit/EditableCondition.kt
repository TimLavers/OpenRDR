package io.rippledown.model.condition.edit

import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.True
import kotlinx.serialization.Serializable

@Serializable
enum class Type {
    Text {
        override fun convert(value: String) = value
    },
    Integer {
        override fun convert(value: String) = value.toIntOrNull()
    },
    Real {
        override fun convert(value: String) = value.toDoubleOrNull()
    };

    fun valid(value: String) = convert(value) != null
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
    fun prerequisite(): Condition = True
    fun shouldBeUsedAtMostOncePerRule() = true
}

