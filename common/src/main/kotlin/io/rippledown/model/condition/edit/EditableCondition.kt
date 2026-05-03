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

    /**
     * When true, a suggestion offering this editable condition is only
     * surfaced if the condition, evaluated with its auto-populated
     * initial value, actually holds for the session case. When false
     * (e.g. for `does not contain ""` where the initial value is an
     * always-placeholder empty string the user must edit), the initial
     * value is treated as a placeholder and the check is skipped — the
     * prerequisite alone governs whether the suggestion appears.
     */
    fun initialValueRepresentsHoldingCondition(): Boolean = true
}

