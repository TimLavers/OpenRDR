package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class Attribute(val name: String, val id: Int = 1) {
    init {
        check(name.isNotEmpty()) {
            "Attribute names cannot be blank."
        }
        check(name.length < 256) {
            "Attribute names cannot have length more than 255."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Attribute

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }


}