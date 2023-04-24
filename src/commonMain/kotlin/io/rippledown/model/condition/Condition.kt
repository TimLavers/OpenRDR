package io.rippledown.model.condition

import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase

// ORD1
@kotlinx.serialization.Serializable
sealed class Condition() {
    abstract val id: Int?
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
    open fun alignAttributes(idToAttribute: (Int) -> Attribute): Condition = this
    abstract fun sameAs(other: Condition): Boolean
}

