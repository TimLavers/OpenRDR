package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

const val HAS_CURRENT_VALUE = "has a current value"

@Serializable
data class HasCurrentValue(override val id: Int? = null, val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val latest = case.getLatest(attribute) ?: return false
        return latest.value.text.isNotBlank()
    }

    override fun asText() = "${attribute.name} $HAS_CURRENT_VALUE"

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = HasCurrentValue(id, idToAttribute(attribute.id))

    override fun sameAs(other: Condition): Boolean {
        return if (other is HasCurrentValue) {
            other.attribute == attribute
        } else false
    }
}