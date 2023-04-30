package io.rippledown.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

fun convertNameToId(name: String): String {
    val stripped = name.lowercase().filter { it.isLetterOrDigit() }
    val randomPart = Random.nextInt(10_000_000)
    return "${stripped}_$randomPart"
}

@Serializable
class KBInfo(val id: String, val name: String) {
    constructor(name: String): this(convertNameToId(name), name)

    init {
        require(id.length < 128) {
            "KBInfo ids have maximum length 127."
        }
        require(id.isNotEmpty()) {
            "KBInfo id cannot be blank."
        }
        require("[a-z0-9_]+".toRegex().matches(id)) {
            "KBInfo id should consist of letters, numbers, and _ only, but got $id."
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