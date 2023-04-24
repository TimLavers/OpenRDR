package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class GreaterThanOrEqualTo(override val id: Int? = null, val attribute: Attribute, val d: Double) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        val real = latest.value.real ?: return false
        return real >= d
    }

    override fun asText(): String {
        return "${attribute.name} ≥ $d"
    }

    override fun sameAs(other: Condition): Boolean {
        return if (other is GreaterThanOrEqualTo) {
            other.attribute == attribute && other.d.compareTo(d) == 0
        } else false
    }
}