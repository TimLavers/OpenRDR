package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class Is(override val id: Int? = null, val attribute: Attribute, val toFind: String) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.value.text == toFind
    }

    override fun asText(): String {
        return "${attribute.name} is \"$toFind\""
    }

    override fun sameAs(other: Condition): Boolean {
        return if (other is Is) {
            other.attribute == attribute
        } else false
    }
}