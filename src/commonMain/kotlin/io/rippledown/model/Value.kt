package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class Value(val value: String) {
    val real: Float? by lazy {
        value.toFloatOrNull()
    }
}