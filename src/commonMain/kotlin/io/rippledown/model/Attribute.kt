package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class Attribute(val name: String) {
    init {
        check(name.isNotEmpty()) {
            "Attribute names cannot be blank."
        }
        check(name.length < 256) {
            "Attribute names cannot have length more than 255."
        }
    }
}