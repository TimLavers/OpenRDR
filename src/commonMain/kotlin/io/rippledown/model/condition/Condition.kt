package io.rippledown.model.condition

import io.rippledown.model.RDRCase

// ORD1
@kotlinx.serialization.Serializable
sealed class Condition {
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
}

