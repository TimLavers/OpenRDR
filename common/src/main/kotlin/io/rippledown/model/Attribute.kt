package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
/**
 * An attribute is identified by its [id], so its [name] can be changed
 * without affecting anything that refers to it. The name is renamed in
 * place (see `AttributeManager.rename`) so that every holder of the
 * attribute sees the new name; holders that were deserialized separately
 * are aligned with the attribute manager when a knowledge base is loaded.
 */
@Serializable
data class Attribute(val id: Int, var name: String, val kind: AttributeKind = AttributeKind.EXTERNAL) {
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