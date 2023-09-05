package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

abstract class RDCondition {
    abstract val id: Int?
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
    abstract fun alignAttributes(idToAttribute: (Int) -> Attribute): RDCondition
    abstract fun sameAs(other: RDCondition): Boolean
}
