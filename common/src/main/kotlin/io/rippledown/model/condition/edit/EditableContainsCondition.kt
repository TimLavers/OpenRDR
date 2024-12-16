package io.rippledown.model.condition.edit

import io.rippledown.model.Attribute
import io.rippledown.model.condition.EpisodicCondition
import io.rippledown.model.condition.episodic.predicate.Contains
import io.rippledown.model.condition.episodic.predicate.IsNotBlank
import io.rippledown.model.condition.episodic.signature.Current
import io.rippledown.model.condition.episodic.signature.Signature
import kotlinx.serialization.Serializable

@Serializable
data class EditableContainsCondition(val attribute: Attribute,
                                     val initialValue: String,
                                     val signature: Signature = Current): EditableCondition {
    private val initialEditableValue = EditableValue(initialValue, Type.Text)

    override fun fixedTextPart1() = condition("").asText().dropLast(2)

    override fun shouldBeUsedAtMostOncePerRule() = false

    override fun editableValue() = initialEditableValue

    override fun condition(value: String) =
        EpisodicCondition(attribute = attribute, predicate = Contains(value), signature = signature)

    override fun prerequisite() =
        EpisodicCondition(attribute = attribute, predicate = IsNotBlank, signature = signature)
}