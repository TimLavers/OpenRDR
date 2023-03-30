package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
class KBInfo(val id: String, val name: String) {
    constructor(name: String): this(name, name)

    init {
        require(id.length < 128) {
            "KBInfo ids have maximum length 127."
        }
        require(id.isNotEmpty()) {
            "KBInfo id cannot be blank."
        }
        require("[a-zA-Z0-9-]+".toRegex().matches(id)) {
            "KBInfo id should consist of letters, numbers, and - only."
        }
        require(name.isNotEmpty()) {
            "KBInfo name cannot be blank."
        }
        require(name.length < 128) {
            "KBInfo names have maximum length 127."
        }
        require(!name.contains('\n')) {
            "KBInfo name cannot contain a newline."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KBInfo

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "KBInfo(id='$id', name='$name')"
    }
}