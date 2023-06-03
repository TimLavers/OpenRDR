package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class IsLow(override val id: Int? = null, val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.isLow()
    }

    override fun asText(): String {
        return "${attribute.name} is low"
    }

    override fun sameAs(other: Condition): Boolean {
        return if (other is IsLow) {
            other.attribute == attribute
        } else false
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = IsLow(id, idToAttribute(attribute.id))
}