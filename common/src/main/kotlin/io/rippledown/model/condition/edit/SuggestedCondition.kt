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
    fun shouldBeSuggestedForCase(case: RDRCase): Boolean
    fun shouldBeUsedAtMostOncePerRule() = true
}
@Serializable
data class NonEditableSuggestedCondition(val initialSuggestion: Condition, val filter: Condition = initialSuggestion): SuggestedCondition {
    override fun initialSuggestion() = initialSuggestion

    override fun shouldBeSuggestedForCase(case: RDRCase) = filter.holds(case)

    override fun isEditable() = false

    override fun editableCondition(): EditableCondition? = null
}
@Serializable
data class EditableSuggestedCondition(val editableCondition: EditableCondition): SuggestedCondition {
    override fun initialSuggestion() = editableCondition.condition(editableCondition.editableValue().value)
    override fun shouldBeSuggestedForCase(case: RDRCase) = editableCondition.prerequisite().holds(case)
    override fun isEditable() = true

    override fun editableCondition() = editableCondition

    override fun shouldBeUsedAtMostOncePerRule() = editableCondition.shouldBeUsedAtMostOncePerRule()
}