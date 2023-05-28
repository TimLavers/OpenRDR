package io.rippledown.model.condition

import io.rippledown.model.RDRCase
import kotlinx.serialization.Serializable

// ORD1
@Serializable
sealed class Condition {
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
}

