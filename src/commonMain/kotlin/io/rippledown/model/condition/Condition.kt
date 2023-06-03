package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

// ORD1
@Serializable
sealed class Condition {
    abstract val id: Int?
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
    abstract fun alignAttributes(idToAttribute: (Int) -> Attribute): Condition
    abstract fun sameAs(other: Condition): Boolean
}
