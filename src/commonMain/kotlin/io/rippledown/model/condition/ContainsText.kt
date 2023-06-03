package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class ContainsText(override val id: Int? = null, val attribute: Attribute, val toFind: String) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.value.text.contains(toFind)
    }

    override fun asText(): String {
        return "${attribute.name} contains \"$toFind\""
    }

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = ContainsText(id, idToAttribute(attribute.id), toFind)

    override fun sameAs(other: Condition): Boolean {
        return if (other is ContainsText) {
            other.attribute == attribute && other.toFind == toFind
        } else false
    }
}