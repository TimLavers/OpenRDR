package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class GreaterThanOrEqualTo(val attribute: Attribute, val d: Double) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        val real = latest.value.real ?: return false
        return real >= d
    }

    override fun asText(): String {
        return "${attribute.name} >= \"$d\""
    }
}