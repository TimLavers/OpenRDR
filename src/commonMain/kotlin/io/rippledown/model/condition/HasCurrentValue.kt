package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

@Serializable
data class HasCurrentValue(override val id: Int? = null, val attribute: Attribute) : Condition() {
    override fun holds(case: RDRCase) = HasNoCurrentValue(attribute).holds(case).not()

    override fun asText() = "${attribute.name} has a current value"

    override fun alignAttributes(idToAttribute: (Int) -> Attribute) = HasCurrentValue(id, idToAttribute(attribute.id))

    override fun sameAs(other: Condition): Boolean {
        return if (other is HasCurrentValue) {
            other.attribute == attribute
        } else false
    }
}