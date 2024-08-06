package io.rippledown.model.condition.edit

import io.rippledown.model.condition.Condition
import kotlinx.serialization.Serializable

@Serializable
sealed interface SuggestedCondition {
    fun initialSuggestion(): Condition
    fun isEditable(): Boolean
    fun editableCondition(): EditableCondition?
}
data class FixedSuggestedCondition(val initialSuggestion: Condition): SuggestedCondition {
    override fun initialSuggestion(): Condition {
        return initialSuggestion
    }

    override fun isEditable(): Boolean {
        return false
    }

    override fun editableCondition(): EditableCondition? = null
}
data class EditableSuggestedCondition(val initialSuggestion: Condition, val editableCondition: EditableCondition): SuggestedCondition {
    override fun initialSuggestion() = initialSuggestion

    override fun isEditable() = true

    override fun editableCondition() = editableCondition
}