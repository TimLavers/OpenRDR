package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class Value(val text: String) {
    val real: Float? by lazy {
        text.toFloatOrNull()
    }
}