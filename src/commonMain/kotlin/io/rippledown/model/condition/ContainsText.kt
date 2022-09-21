package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class ContainsText(val attribute: Attribute, val toFind: String) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.value.text.contains(toFind)
    }

    override fun asText(): String {
        return "${attribute.name} contains \"$toFind\""
    }
}