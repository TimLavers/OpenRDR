package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class Is(val attribute: Attribute, val toFind: String) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        println("Is: attr text: '${latest.value.text}', toFind: '$toFind'")
        return latest.value.text == toFind
    }

    override fun asText(): String {
        return "${attribute.name} is \"$toFind\""
    }
}