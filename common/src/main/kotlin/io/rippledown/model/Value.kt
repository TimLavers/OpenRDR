package io.rippledown.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Value(val text: String) {
    @Transient
    val real = text.toDoubleOrNull()
}