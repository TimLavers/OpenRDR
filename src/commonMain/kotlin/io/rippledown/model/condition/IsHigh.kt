package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class IsHigh(override val id: Int? = null, val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.isHigh()
    }

    override fun asText(): String {
        return "${attribute.name} is high"
    }

    override fun sameAs(other: Condition): Boolean {
        return if (other is IsHigh) {
            other.attribute == attribute
        } else false
    }
}