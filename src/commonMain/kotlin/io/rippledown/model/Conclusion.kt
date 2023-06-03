package io.rippledown.model

import kotlinx.serialization.Serializable

// ORD1
@Serializable
data class Conclusion(val id: Int, val text: String) {
    init {
        check(text.isNotEmpty()) {
            "Conclusions cannot be blank."
        }
        check(text.length < 2049) {
            "Conclusions have maximum length 2048."
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Conclusion

        return id == other.id
    }

    override fun hashCode(): Int {
        return id
    }
}