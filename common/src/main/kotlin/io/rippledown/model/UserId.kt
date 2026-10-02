package io.rippledown.model

@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "A user id cannot be blank." }
    }

    override fun toString() = value
}
