package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase

@kotlinx.serialization.Serializable
data class HasNoCurrentValue(val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute)
        if (latest == null) return true else return latest.value.text.isBlank()
    }

    override fun asText(): String {
        return "${attribute.name} has no current value"
    }
}