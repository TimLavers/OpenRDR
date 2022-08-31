package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase

@kotlinx.serialization.Serializable
data class IsNormal(val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute)
        if (latest == null) return false else return latest.isNormal()
    }

    override fun asText(): String {
        return "${attribute.name} is normal"
    }
}