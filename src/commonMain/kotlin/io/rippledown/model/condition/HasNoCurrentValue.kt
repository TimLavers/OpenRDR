package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class HasNoCurrentValue(override val id: Int? = null, val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return true
        return latest.value.text.isBlank()
    }

    override fun asText(): String {
        return "${attribute.name} has no current value"
    }

    override fun sameAs(other: Condition): Boolean {
        return if (other is HasNoCurrentValue) {
            other.attribute == attribute
        } else false
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = HasNoCurrentValue(id, idToAttribute(attribute.id))
}