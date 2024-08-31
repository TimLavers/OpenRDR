package io.rippledown.model.condition.edit

import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import kotlinx.serialization.Serializable

@Serializable
sealed interface SuggestedCondition {
    fun initialSuggestion(): Condition
    fun isEditable(): Boolean
    fun editableCondition(): EditableCondition?
    fun asText() = initialSuggestion().asText()
    fun shouldBeSuggestedForCase(case: RDRCase) = isEditable() || initialSuggestion().holds(case)
}
@Serializable
data class FixedSuggestedCondition(val initialSuggestion: Condition): SuggestedCondition {
    override fun initialSuggestion(): Condition {
        return initialSuggestion
    }

    override fun isEditable(): Boolean {
        return false
    }

    override fun editableCondition(): EditableCondition? = null
}
@Serializable
data class EditableSuggestedCondition(val editableCondition: EditableCondition): SuggestedCondition {
    override fun initialSuggestion() = editableCondition.condition(editableCondition.editableValue().value)

    override fun isEditable() = true

    override fun editableCondition() = editableCondition
}