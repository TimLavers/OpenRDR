package io.rippledown.model.condition.edit

import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.True
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
data class NonEditableSuggestedCondition(val initialSuggestion: Condition, val filter: Condition = True) :
    SuggestedCondition {
    override fun initialSuggestion() = initialSuggestion

    override fun shouldBeSuggestedForCase(case: RDRCase) = filter.holds(case) && initialSuggestion.holds(case)

    override fun isEditable() = false

    override fun editableCondition(): EditableCondition? = null
}

@Serializable
data class EditableSuggestedCondition(val editableCondition: EditableCondition) : SuggestedCondition {
    override fun initialSuggestion() = editableCondition.condition(editableCondition.editableValue().value)

    /**
     * An editable suggestion is only offered when
     *  1. its prerequisite holds for the case (e.g. the attribute is
     *     numeric for a cutoff condition), AND
     *  2. the suggestion, evaluated with its auto-populated initial
     *     value, actually holds for the session case.
     *
     * Without (2) we end up suggesting conditions that are plainly false
     * for the case the user is looking at — e.g. `all HAEMOGLOBIN >= 194`
     * when only the current episode is at 194 and an earlier one was
     * lower. The user can still edit the value afterwards; we just don't
     * seed the list with falsehoods.
     */
    override fun shouldBeSuggestedForCase(case: RDRCase): Boolean {
        if (!editableCondition.prerequisite().holds(case)) return false
        if (!editableCondition.initialValueRepresentsHoldingCondition()) return true
        return initialSuggestion().holds(case)
    }

    override fun isEditable() = true

    override fun editableCondition() = editableCondition

    override fun shouldBeUsedAtMostOncePerRule() = editableCondition.shouldBeUsedAtMostOncePerRule()
}