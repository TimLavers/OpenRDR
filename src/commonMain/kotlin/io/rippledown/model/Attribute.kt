package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class Attribute(val id: Int, val name: String) {
    init {
        check(name.isNotEmpty()) {
            "Attribute names cannot be blank."
        }
        check(name.length < 256) {
            "Attribute names cannot have length more than 255."
        }
    }

    fun isEquivalent(other: Attribute) = name == other.name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Attribute

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}