package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class HasCurrentValue(val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.value.text.isNotBlank()
    }

    override fun asText(): String {
        return "${attribute.name} has a current value"
    }
}